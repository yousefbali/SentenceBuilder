/******************************************************************************
 * BigramAutocompleteAlgorithm
 *
 * Autocomplete strategy that ranks suggestions based on bigram frequency.
 * Given the last word the user typed, this algorithm:
 *
 *   1. Normalizes the word to lower-case (matching the tokenizer/DB).
 *   2. Looks up all bigram transitions of the form (lastWord -> nextWord).
 *   3. Returns the top-N next words ordered by their bigram frequency.
 *
 * This behaves like a simple "next word predictor" driven purely by bigrams.
 *
 * Written by Ali Saidane (axs220579) for CS 4485, Sentence Builder project,
 * starting October 22, 2025.
 ******************************************************************************/

package com.sentencebuilder.algorithms.autocomplete;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bigram-based Top-N autocomplete.  This algorithm is deterministic:
 * repeated calls with the same context will produce the same ordered list
 * because we always sort by bigram frequency descending.
 */
public class BigramAutocompleteAlgorithm implements AutocompleteAlgorithm {

    /**
     * Human-readable label for the JavaFX ComboBox.
     */
    @Override
    public String name() {
        return "Bigram Top-N";
    }

    /**
     * Suggest the most likely next words given the last word the user typed,
     * using bigram counts from the database.
     *
     * @param c        Open JDBC connection.
     * @param lastWord Last token typed by the user.  We assume this is already
     *                 a single word (the UI extracts the last token), but we
     *                 still trim and lowercase for safety.
     * @param limit    Maximum number of suggestions to return.  If {@code limit}
     *                 is less than or equal to zero, this returns an empty list.
     *
     * @return Up to {@code limit} suggestions ordered by bigram frequency, or
     *         an empty list if no bigrams exist for the given word.
     *
     * @throws Exception If a database error occurs.
     */
    @Override
    public List<String> suggest(Connection c, String lastWord, int limit) throws Exception {
        if (lastWord == null || lastWord.isBlank() || limit <= 0) {
            // Nothing sensible to suggest.
            return List.of();
        }

        // Normalize to the canonical form used in the DB.
        String normalized = lastWord.trim().toLowerCase(Locale.ROOT);

        String sql =
                "SELECT w2.word_text " +
                "FROM words w1 " +
                "JOIN word_relations_bigram r ON w1.word_id = r.from_word_id " +
                "JOIN words w2 ON w2.word_id = r.to_word_id " +
                "WHERE w1.word_text = ? " +
                "ORDER BY r.frequency DESC " +
                "LIMIT ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<String> suggestions = new ArrayList<>();
                // Each row is a distinct candidate next word, already sorted
                // by how frequently it follows the given lastWord in the corpus.
                while (rs.next()) {
                    suggestions.add(rs.getString(1));
                }
                return suggestions;
            }
        }
    }
}

