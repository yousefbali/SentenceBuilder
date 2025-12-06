/******************************************************************************
 * ContextTrigramAutocompleteAlgorithm
 *
 * Autocomplete strategy that uses as much context as is available:
 *
 *   - If the user has typed at least two words, use trigram statistics:
 *       (word_{n-2}, word_{n-1}) -> suggestions for word_n
 *   - If only one word is available, fall back to bigram statistics:
 *       (word_{n-1}) -> suggestions for word_n
 *
 * Suggestions are ranked purely by n-gram frequency (no sampling), so the
 * result is deterministic for a given context.
 *
 * Written by Yoel Kidane (yxk220039) for CS 4485, Sentence Builder project,
 * starting October 18, 2025.
 ******************************************************************************/

package com.sentencebuilder.algorithms.autocomplete;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Context-aware autocomplete that prefers trigram continuations when the
 * user has typed at least two words, otherwise falls back to bigram data.
 */
public class ContextTrigramAutocompleteAlgorithm implements AutocompleteAlgorithm {

    /**
     * Human-readable label for the JavaFX ComboBox.
     */
    @Override
    public String name() {
        return "Context trigram (with bigram fallback)";
    }

    /**
     * Suggest words using a two-word trigram context if possible, or a one-word
     * bigram context otherwise.
     *
     * @param c        Open JDBC connection.
     * @param textSoFar Entire contents of the input field (possibly multiple
     *                  words separated by whitespace).  We extract the last
     *                  one or two tokens from this string.
     * @param limit    Maximum number of suggestions to return.  If {@code limit}
     *                 is less than or equal to zero, this returns an empty list.
     *
     * @return Up to {@code limit} suggestions ordered by descending n-gram
     *         frequency.  If no n-grams exist for the context, returns an
     *         empty list.
     *
     * @throws Exception If a database error occurs.
     */
    @Override
    public List<String> suggest(Connection c, String textSoFar, int limit) throws Exception {
        if (textSoFar == null || textSoFar.isBlank() || limit <= 0) {
            return List.of();
        }

        // Split on whitespace and normalize tokens to match how we store them
        // in the database (lower-case, no empty strings).
        String[] rawTokens = textSoFar.trim().split("\\s+");
        if (rawTokens.length == 0) {
            return List.of();
        }

        String last = normalize(rawTokens[rawTokens.length - 1]);
        if (last.isEmpty()) {
            return List.of();
        }

        // If we have at least two tokens, attempt a trigram-based suggestion
        // using the last two words as context.  Otherwise, go straight to
        // bigram-based suggestions using only the last word.
        if (rawTokens.length >= 2) {
            String prev = normalize(rawTokens[rawTokens.length - 2]);
            if (!prev.isEmpty()) {
                List<String> trigramSuggestions = suggestFromTrigrams(c, prev, last, limit);
                if (!trigramSuggestions.isEmpty()) {
                    return trigramSuggestions;
                }
            }
        }

        // Trigram context unavailable or yielded nothing: fall back to bigrams
        // based only on the last word.
        return suggestFromBigrams(c, last, limit);
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    /**
     * Normalize a token to the canonical format used in the DB:
     * lower-case and trimmed.  Returns an empty string if the input is null.
     */
    private static String normalize(String token) {
        if (token == null) {
            return "";
        }
        return token.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Suggest words using trigram statistics of the form:
     *   (prev2, prev1) -> next
     *
     * @param c      Open JDBC connection.
     * @param prev2  Token at position n-2.
     * @param prev1  Token at position n-1.
     * @param limit  Maximum number of suggestions to return.
     *
     * @return Up to {@code limit} next words ordered by trigram frequency,
     *         or an empty list if no trigram entries exist for this context.
     *
     * @throws Exception If a database error occurs.
     */
    private List<String> suggestFromTrigrams(Connection c, String prev2, String prev1, int limit)
            throws Exception {

        String sql = """
            SELECT w3.word_text
            FROM words w1
            JOIN word_relations_trigram t ON w1.word_id = t.first_word_id
            JOIN words w2 ON w2.word_id = t.second_word_id
            JOIN words w3 ON w3.word_id = t.next_word_id
            WHERE w1.word_text = ?
              AND w2.word_text = ?
            ORDER BY t.frequency DESC
            LIMIT ?
            """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prev2);
            ps.setString(2, prev1);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<String> suggestions = new ArrayList<>();
                while (rs.next()) {
                    suggestions.add(rs.getString(1));
                }
                return suggestions;
            }
        }
    }

    /**
     * Suggest words using bigram statistics of the form:
     *   (prev) -> next
     *
     * @param c      Open JDBC connection.
     * @param prev   Last token typed by the user.
     * @param limit  Maximum number of suggestions to return.
     *
     * @return Up to {@code limit} words ordered by bigram frequency,
     *         or an empty list if no bigram entries exist for this context.
     *
     * @throws Exception If a database error occurs.
     */

    private List<String> suggestFromBigrams(Connection c, String prev, int limit) throws Exception {
        String sql = """
            SELECT w2.word_text
            FROM words w1
            JOIN word_relations_bigram r ON w1.word_id = r.from_word_id
            JOIN words w2 ON w2.word_id = r.to_word_id
            WHERE w1.word_text = ?
            ORDER BY r.frequency DESC
            LIMIT ?
            """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prev);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<String> suggestions = new ArrayList<>();
                while (rs.next()) {
                    suggestions.add(rs.getString(1));
                }
                return suggestions;
            }
        }
    }
}

