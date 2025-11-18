package com.sentencebuilder.algorithms.sentence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Greedy trigram-based sentence generator.
 *
 * Logic:
 *  - If the starting text has 2+ words, use the LAST TWO as the trigram context.
 *  - If it has 1 word, first pick a second word using the bigram table,
 *    then switch to trigram continuation.
 *  - At each step, pick the most frequent next word given (w1, w2).
 *  - If no trigram is found, optionally fall back to a bigram on w2.
 */
public class TrigramGreedySentenceAlgorithm implements SentenceAlgorithm {

    @Override
    public String name() {
        return "Trigram Greedy";
    }

    @Override
    public String generate(Connection c, String startingWord, int maxWords) throws Exception {
        if (startingWord == null) {
            return "";
        }
        String trimmed = startingWord.trim();
        if (trimmed.isEmpty() || maxWords <= 0) {
            return "";
        }

        String[] tokens = trimmed.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder(trimmed);

        String w1;
        String w2;
        int count = tokens.length;

        if (tokens.length == 1) {
            // Only one starting word: use bigram to get an initial successor
            w1 = tokens[0];
            String next = mostFrequentBigramSuccessor(c, w1);
            if (next == null) {
                // No continuation possible
                return sb.toString();
            }
            sb.append(" ").append(next);
            w2 = next;
            count++;
        } else {
            // Use the last two words as trigram context
            w1 = tokens[tokens.length - 2];
            w2 = tokens[tokens.length - 1];
        }

        // Trigram loop
        while (count < maxWords) {
            String next = mostFrequentTrigramSuccessor(c, w1, w2);

            if (next == null) {
                // Optional fallback: try bigram on the last word
                next = mostFrequentBigramSuccessor(c, w2);
                if (next == null) {
                    break; // nothing more to do
                }
            }

            sb.append(" ").append(next);
            count++;

            // Slide the trigram window
            w1 = w2;
            w2 = next;
        }

        return sb.toString();
    }

    /**
     * Returns the most frequent next word for the bigram (lastWord, ?),
     * or null if none exists.
     */
    private String mostFrequentBigramSuccessor(Connection c, String lastWord) throws SQLException {
        String sql =
                "SELECT w2.word_text " +
                "FROM words w1 " +
                "JOIN word_relations_bigram r ON w1.word_id = r.from_word_id " +
                "JOIN words w2 ON w2.word_id = r.to_word_id " +
                "WHERE w1.word_text = ? " +
                "ORDER BY r.frequency DESC " +
                "LIMIT 1";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, lastWord.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        }
    }

    /**
     * Returns the most frequent next word for the trigram (firstWord, secondWord, ?),
     * or null if none exists.
     */
    private String mostFrequentTrigramSuccessor(Connection c, String firstWord, String secondWord)
            throws SQLException {

        String sql =
                "SELECT w3.word_text " +
                "FROM words w1 " +
                "JOIN word_relations_trigram r " +
                "  ON w1.word_id = r.first_word_id " +
                "JOIN words w2 " +
                "  ON w2.word_id = r.second_word_id " +
                "JOIN words w3 " +
                "  ON w3.word_id = r.next_word_id " +
                "WHERE w1.word_text = ? " +
                "  AND w2.word_text = ? " +
                "ORDER BY r.frequency DESC " +
                "LIMIT 1";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, firstWord.toLowerCase(Locale.ROOT));
            ps.setString(2, secondWord.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        }
    }
}
