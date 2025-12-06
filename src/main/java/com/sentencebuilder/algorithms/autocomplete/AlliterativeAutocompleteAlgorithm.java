/******************************************************************************
 * AlliterativeAutocompleteAlgorithm
 *
 * Autocomplete strategy that suggests words which start with the same letter
 * as the last word the user typed.  Unlike bigram-based strategies, this
 * deliberately ignores word order and focuses on "alliterative" fun.
 *
 * Given the last word, this algorithm:
 *
 *   1. Normalizes it to lower-case.
 *   2. Extracts its first character; if it's not a letter, returns no results.
 *   3. Returns the most frequent words in the corpus whose text starts with
 *      that same letter, excluding the seed word itself.
 *
 * Written by Yoel Kidane (yxk220039) for CS 4485, Sentence Builder project,
 * starting November 11, 2025.
 ******************************************************************************/

package com.sentencebuilder.algorithms.autocomplete;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Alliterative autocomplete implementation.  This is less "smart" than the
 * bigram-based strategies, but it is useful for exploring the vocabulary in
 * the corpus and adds a playful mode to the UI.
 */
public class AlliterativeAutocompleteAlgorithm implements AutocompleteAlgorithm {

    /**
     * Human-readable label for the JavaFX ComboBox.
     */
    @Override
    public String name() {
        return "Alliterative (same first letter)";
    }

    /**
     * Suggest words that start with the same first letter as the last word
     * the user typed.  Only letter-first tokens are considered; for numeric
     * or punctuation-starting "words" we return no suggestions.
     *
     * @param c        Open JDBC connection.
     * @param lastWord Last token typed by the user, as a raw string.
     * @param limit    Maximum number of suggestions to return.  If {@code limit}
     *                 is less than or equal to zero, this returns an empty list.
     *
     * @return A list of words which start with the same letter as {@code lastWord},
     *         ordered by descending total frequency in the corpus.  The original
     *         seed word is not included in the suggestions.
     *
     * @throws Exception If a database error occurs.
     */
    @Override
    public List<String> suggest(Connection c, String lastWord, int limit) throws Exception {
        if (lastWord == null || lastWord.isBlank() || limit <= 0) {
            return List.of();
        }

        // Normalize to the canonical form used by the tokenizer/database.
        String seed = lastWord.trim().toLowerCase(Locale.ROOT);
        char first = seed.charAt(0);

        // If the token does not start with a letter (e.g., "3d", "?"), there
        // is no meaningful "alliterative" cohort, so we do not suggest any.
        if (!Character.isLetter(first)) {
            return List.of();
        }

        String sql =
                "SELECT word_text " +
                "FROM words " +
                "WHERE word_text LIKE ? " +
                "  AND word_text <> ? " +  // avoid suggesting the exact same word back
                "ORDER BY total_count DESC " +
                "LIMIT ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, first + "%");
            ps.setString(2, seed);
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
}

