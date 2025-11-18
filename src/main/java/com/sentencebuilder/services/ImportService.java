
package com.sentencebuilder.services;

import com.sentencebuilder.dao.Database;
import com.sentencebuilder.tokenize.TextTokenizer;
import com.sentencebuilder.tokenize.TokenizationResult;

import java.io.File;
import java.sql.*;
import java.util.Map;

public class ImportService {

    public void importFile(File file, TextTokenizer tokenizer) throws Exception {
        if (file == null) throw new IllegalArgumentException("File is null");

        TokenizationResult result = tokenizer.tokenize(file);

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);

            int fileId = upsertFile(c, file.getName(), result.getWordCount());

            for (Map.Entry<String, TokenizationResult.WordStats> e : result.getWordStats().entrySet()) {
                int wordId = upsertWord(c, e.getKey(), e.getValue());
                upsertWordFile(c, wordId, fileId, e.getValue().totalCount());
            }

            for (Map.Entry<TokenizationResult.Bigram, Integer> e : result.getBigrams().entrySet()) {
                int fromId = getWordId(c, e.getKey().from());
                int toId = getWordId(c, e.getKey().to());
                upsertBigram(c, fromId, toId, e.getValue());
            }

            for (Map.Entry<TokenizationResult.Trigram, Integer> e : result.getTrigrams().entrySet()) {
                int firstId = getWordId(c, e.getKey().first());
                int secondId = getWordId(c, e.getKey().second());
                int nextId = getWordId(c, e.getKey().next());
                upsertTrigram(c, firstId, secondId, nextId, e.getValue());
            }

            c.commit();
        }
    }

    private int upsertFile(Connection c, String filename, int wordCount) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO files(filename, word_count, import_date) VALUES(?,?,NOW()) " +
                        "ON DUPLICATE KEY UPDATE word_count=?, import_date=NOW()", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, filename);
            ps.setInt(2, wordCount);
            ps.setInt(3, wordCount);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT file_id FROM files WHERE filename=?")) {
            ps.setString(1, filename);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to upsert file: " + filename);
    }

    private int upsertWord(Connection c, String word, TokenizationResult.WordStats stats) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO words(word_text, total_count, sentence_start_count, sentence_end_count) VALUES(?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE total_count=total_count+VALUES(total_count), " +
                        "sentence_start_count=sentence_start_count+VALUES(sentence_start_count), " +
                        "sentence_end_count=sentence_end_count+VALUES(sentence_end_count)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, word);
            ps.setInt(2, stats.totalCount());
            ps.setInt(3, stats.sentenceStartCount());
            ps.setInt(4, stats.sentenceEndCount());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return getWordId(c, word);
    }

    private int getWordId(Connection c, String word) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT word_id FROM words WHERE word_text=?")) {
            ps.setString(1, word);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Missing word in DB: " + word);
    }

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

    private void upsertTrigram(Connection c, int firstId, int secondId, int nextId, int inc) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO word_relations_trigram(first_word_id, second_word_id, next_word_id, frequency) VALUES(?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE frequency = frequency + VALUES(frequency)")) {
            ps.setInt(1, firstId);
            ps.setInt(2, secondId);
            ps.setInt(3, nextId);
            ps.setInt(4, inc);
            ps.executeUpdate();
        }
    }
}
