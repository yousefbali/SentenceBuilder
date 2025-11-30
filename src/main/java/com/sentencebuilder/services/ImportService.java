/******************************************************************************
 * ImportService
 *
 * Service responsible for taking the output of a TextTokenizer and writing
 * its statistics into the Sentence Builder database schema.
 *
 * Responsibilities:
 *   - Validate the input file.
 *   - Call a TextTokenizer to compute word / bigram / trigram stats.
 *   - Upsert rows into:
 *       * files
 *       * words
 *       * word_files
 *       * word_relations_bigram
 *       * word_relations_trigram
 *   - Perform all writes inside a single database transaction so that either
 *     the entire file is imported or nothing is.
 *   - Return an ImportSummary so the UI can show quick feedback.
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.services;

import com.sentencebuilder.dao.Database;
import com.sentencebuilder.tokenize.TextTokenizer;
import com.sentencebuilder.tokenize.TokenizationResult;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Service layer for importing a single text file into the Sentence Builder DB.
 */
public class ImportService {

    /**
     * Summary of what got imported. Handy for UI feedback after import.
     *
     * @param filename      name of the imported file (no path).
     * @param totalTokens   total number of tokens in the file.
     * @param distinctWords number of unique words observed.
     * @param bigramCount   total number of bigram relations inserted/updated.
     * @param trigramCount  total number of trigram relations inserted/updated.
     */
    public record ImportSummary(
            String filename,
            int totalTokens,
            int distinctWords,
            int bigramCount,
            int trigramCount
    ) {}

    /**
     * Import a single text file into the DB using the given tokenizer.
     *
     * Steps:
     *   1. Validate that the file exists and is a regular file.
     *   2. Tokenize the file into a TokenizationResult (word + n-gram stats).
     *   3. Open a DB connection and start a transaction.
     *   4. Upsert:
     *        - the file row itself,
     *        - words + word_files rows,
     *        - bigram relations,
     *        - trigram relations.
     *   5. Commit on success, rollback on failure.
     *
     * @param file      plain-text file to import.
     * @param tokenizer tokenizer implementation to use.
     * @return ImportSummary describing what was imported.
     * @throws Exception if tokenization or DB operations fail.
     */
    public ImportSummary importFile(File file, TextTokenizer tokenizer) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Not a regular file: " + file.getAbsolutePath());
        }

        // 1) Run the tokenizer on the on-disk file.
        TokenizationResult result = tokenizer.tokenize(file);

        // Basic counts used both for DB and UI summary.
        int totalTokens   = result.getWordCount();
        int distinctWords = result.getWordStats().size();
        int bigramCount   = result.getBigrams().values().stream().mapToInt(Integer::intValue).sum();
        int trigramCount  = result.getTrigrams().values().stream().mapToInt(Integer::intValue).sum();

        // 2) Push everything into the database in a single transaction.
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false); // begin transaction

            try {
                // Insert or update the files row; get back the file_id primary key.
                int fileId = upsertFile(c, file.getName(), totalTokens);

                // Cache from word text -> word_id to avoid repeated SELECTs.
                Map<String, Integer> wordIdCache = new HashMap<>();

                // 2a) Insert/update words and file-specific counts in word_files.
                for (Map.Entry<String, TokenizationResult.WordStats> e : result.getWordStats().entrySet()) {
                    String word = e.getKey();
                    TokenizationResult.WordStats stats = e.getValue();

                    // Insert or increment the global word statistics.
                    int wordId = upsertWord(c, word, stats);

                    // Remember this id in the cache so n-gram inserts can use it quickly.
                    wordIdCache.put(word, wordId);

                    // Track how many times this word occurred in this particular file.
                    upsertWordFile(c, wordId, fileId, stats.totalCount());
                }

                // 2b) Insert/update bigram relations using word IDs.
                for (Map.Entry<TokenizationResult.Bigram, Integer> e : result.getBigrams().entrySet()) {
                    TokenizationResult.Bigram b = e.getKey();
                    int inc = e.getValue();

                    // Resolve word_id for each side of the bigram, using the cache when possible.
                    int fromId = getOrLookupWordId(c, wordIdCache, b.from());
                    int toId   = getOrLookupWordId(c, wordIdCache, b.to());

                    upsertBigram(c, fromId, toId, inc);
                }

                // 2c) Insert/update trigram relations using word IDs.
                for (Map.Entry<TokenizationResult.Trigram, Integer> e : result.getTrigrams().entrySet()) {
                    TokenizationResult.Trigram t = e.getKey();
                    int inc = e.getValue();

                    int firstId  = getOrLookupWordId(c, wordIdCache, t.first());
                    int secondId = getOrLookupWordId(c, wordIdCache, t.second());
                    int nextId   = getOrLookupWordId(c, wordIdCache, t.next());

                    upsertTrigram(c, firstId, secondId, nextId, inc);
                }

                // If we reached this point with no exceptions, we commit all changes at once.
                c.commit();
            } catch (Exception ex) {
                // Any exception during import should roll back the partial work.
                c.rollback();
                throw ex;
            }
        }

        // Build a summary for the UI to display.
        return new ImportSummary(
                file.getName(),
                totalTokens,
                distinctWords,
                bigramCount,
                trigramCount
        );
    }

    // Helpers

    /**
     * Get the word_id for a word, using a small in-memory cache to avoid
     * hitting the database repeatedly for the same word.
     *
     * @param c     open connection.
     * @param cache map from word text to word_id.
     * @param word  normalized word text.
     * @return word_id for the given word.
     * @throws SQLException if the word cannot be found in the DB.
     */
    private int getOrLookupWordId(Connection c, Map<String, Integer> cache, String word) throws SQLException {
        Integer cached = cache.get(word);
        if (cached != null) {
            return cached;
        }
        int id = getWordId(c, word);
        cache.put(word, id);
        return id;
    }

    /**
     * Insert a row in the files table or update an existing one, then return
     * the file_id primary key.
     *
     * Behavior:
     *   - If this filename is new, INSERT it and obtain generated key.
     *   - If it already exists, UPDATE word_count + import_date and then
     *     look up the existing file_id.
     */
    private int upsertFile(Connection c, String filename, int wordCount) throws SQLException {
        // Try INSERT with ON DUPLICATE KEY UPDATE first.
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO files(filename, word_count, import_date) VALUES(?,?,NOW()) " +
                        "ON DUPLICATE KEY UPDATE word_count = ?, import_date = NOW()",
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, filename);
            ps.setInt(2, wordCount);
            ps.setInt(3, wordCount);
            ps.executeUpdate();

            // If a new row was inserted, MySQL will give us the generated key.
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        // If we reach here, it likely means the row existed already and no new
        // key was generated, so we manually SELECT the file_id.
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT file_id FROM files WHERE filename = ?")) {
            ps.setString(1, filename);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to upsert file: " + filename);
    }

    /**
     * Insert or update a word row in the words table and return its word_id.
     *
     * Inserts:
     *   - word_text
     *   - total_count
     *   - sentence_start_count
     *   - sentence_end_count
     *
     * On duplicate KEY (same word_text), the counts are incremented by the
     * incoming values, effectively aggregating counts across multiple files.
     */
    private int upsertWord(Connection c, String word, TokenizationResult.WordStats stats) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO words(word_text, total_count, sentence_start_count, sentence_end_count) " +
                        "VALUES(?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        " total_count = total_count + VALUES(total_count), " +
                        " sentence_start_count = sentence_start_count + VALUES(sentence_start_count), " +
                        " sentence_end_count = sentence_end_count + VALUES(sentence_end_count)",
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, word);
            ps.setInt(2, stats.totalCount());
            ps.setInt(3, stats.sentenceStartCount());
            ps.setInt(4, stats.sentenceEndCount());
            ps.executeUpdate();

            // If this was a new insert, retrieve the generated word_id.
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        // If ON DUPLICATE KEY UPDATE fired and no new key was generated,
        // we look up the existing word_id explicitly.
        return getWordId(c, word);
    }

    /**
     * Look up the word_id for the given word_text.
     *
     * @param c    open connection.
     * @param word normalized word text.
     * @return word_id from the words table.
     * @throws SQLException if the word is not found.
     */
    private int getWordId(Connection c, String word) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT word_id FROM words WHERE word_text=?")) {

            ps.setString(1, word);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Missing word in DB: " + word);
    }

    /**
     * Insert or update a row in word_files, which tracks how many times a word
     * appears in a specific file.
     *
     * ON DUPLICATE KEY UPDATE will increment the count if the word/file pair
     * already exists (e.g., if the same file is imported again).
     */
    private void upsertWordFile(Connection c, int wordId, int fileId, int count) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO word_files(word_id, file_id, count_in_file) VALUES(?,?,?) " +
                        "ON DUPLICATE KEY UPDATE count_in_file = count_in_file + VALUES(count_in_file)")) {

            ps.setInt(1, wordId);
            ps.setInt(2, fileId);
            ps.setInt(3, count);
            ps.executeUpdate();
        }
    }

    /**
     * Insert or update a bigram relation in word_relations_bigram.
     *
     * The primary key is typically (from_word_id, to_word_id), so we can use
     * ON DUPLICATE KEY UPDATE to increment the frequency value.
     */
    private void upsertBigram(Connection c, int fromId, int toId, int inc) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO word_relations_bigram(from_word_id, to_word_id, frequency) VALUES(?,?,?) " +
                        "ON DUPLICATE KEY UPDATE frequency = frequency + VALUES(frequency)")) {

            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ps.setInt(3, inc);
            ps.executeUpdate();
        }
    }

    /**
     * Insert or update a trigram relation in word_relations_trigram.
     *
     * The primary key is typically (first_word_id, second_word_id, next_word_id),
     * and frequency is incremented on duplicate.
     */
    private void upsertTrigram(Connection c,
                               int firstId,
                               int secondId,
                               int nextId,
                               int inc) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO word_relations_trigram(first_word_id, second_word_id, next_word_id, frequency) " +
                        "VALUES(?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE frequency = frequency + VALUES(frequency)")) {

            ps.setInt(1, firstId);
            ps.setInt(2, secondId);
            ps.setInt(3, nextId);
            ps.setInt(4, inc);
            ps.executeUpdate();
        }
    }
}
