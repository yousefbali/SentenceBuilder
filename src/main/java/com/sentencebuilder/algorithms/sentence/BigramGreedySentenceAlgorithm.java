/******************************************************************************
 * BigramGreedySentenceAlgorithm
 *
 * Greedy bigram-based sentence generator.
 *
 * Logic:
 *   - Interpret the startingText as a sequence of words; use the LAST word
 *     as the initial bigram context.
 *   - If startingText is empty/null, choose a starting word based on the
 *     words table (preferring high sentence_start_count, then total_count).
 *   - At each step, look up the most frequent bigram continuation:
 *       current_word -> next_word
 *     and append that next_word to the sentence.
 *   - Stop when:
 *       * No continuation exists in the bigram table, or
 *       * maxWords has been reached.
 *
 * This algorithm is intentionally simple and deterministic: for a given corpus
 * and starting text, it will always generate the same continuation.
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.algorithms.sentence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Greedy sentence generator using the word_relations_bigram table.
 */
public class BigramGreedySentenceAlgorithm implements SentenceAlgorithm {

    @Override
    public String name() {
        return "Bigram – Greedy";
    }

    /**
     * Generate a sentence using bigram maximum-frequency continuation.
     */
    @Override
    public String generate(Connection c, String startingText, int maxWords) throws Exception {
        if (maxWords <= 0) {
            return "";
        }

        // Parse the user-supplied starting text into words (simple split on whitespace).
        List<String> words = new ArrayList<>();
        if (startingText != null) {
            String trimmed = startingText.trim();
            if (!trimmed.isEmpty()) {
                words.addAll(Arrays.asList(trimmed.split("\\s+")));
            }
        }

        // If nothing was provided, pick a high-probability starting word
        // (based on sentence_start_count).
        if (words.isEmpty()) {
            String first = pickBestStartingWord(c);
            if (first == null) {
                // No words in DB; nothing sensible to generate.
                return "";
            }
            words.add(first);
        }

        // Bigram context is always the *last* word we have so far.
        while (words.size() < maxWords) {
            String current = words.get(words.size() - 1);
            String next = getMostFrequentBigramSuccessor(c, current);
            if (next == null) {
                break; // No outgoing bigram edges; end the sentence here.
            }
            words.add(next);
        }

        return String.join(" ", words);
    }

    /**
     * Pick a "good" starting word from the corpus.
     * We prefer words that frequently start sentences, falling back on
     * total_count if there is a tie.
     */
/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
    private String pickBestStartingWord(Connection c) throws Exception {
        String sql =
                "SELECT word_text " +
                "FROM words " +
                "ORDER BY sentence_start_count DESC, total_count DESC " +
                "LIMIT 1";

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return null;
    }

    /**
     * Given a current word, find the most frequent bigram continuation:
     *   current_word -> next_word
     *
     * SQL schema:
     *   - words(word_id, word_text, ...)
     *   - word_relations_bigram(from_word_id, to_word_id, frequency)
     */
    private String getMostFrequentBigramSuccessor(Connection c, String currentWord) throws Exception {
        if (currentWord == null || currentWord.isBlank()) {
            return null;
        }

        String normalized = currentWord.toLowerCase(Locale.ROOT);
/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
        String sql =
                "SELECT w2.word_text " +
                "FROM words w1 " +
                "JOIN word_relations_bigram br ON w1.word_id = br.from_word_id " +
                "JOIN words w2 ON br.to_word_id = w2.word_id " +
                "WHERE w1.word_text = ? " +
                "ORDER BY br.frequency DESC, w2.word_text ASC " +
                "LIMIT 1";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalized);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }

        return null;
    }
}

