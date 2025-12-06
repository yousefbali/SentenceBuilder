/******************************************************************************
 * GlobalFrequencyAutocompleteAlgorithm
 *
 * Autocomplete strategy that ignores local context and simply suggests the
 * most common words in the entire corpus.  This is useful as:
 *
 *   - A "fallback" autocomplete when no context is available.
 *   - A quick way to explore the most frequent tokens in the database.
 *
 * Given the last word the user typed, this algorithm does *not* use that
 * word directly; instead, it always returns the top-N words ordered by
 * total_count in the words table.
 *
 * Written by Ali Saidane (axs220579) for CS 4485, Sentence Builder project,
 * starting October 24, 2025.
 ******************************************************************************/

package com.sentencebuilder.algorithms.autocomplete;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Context-free autocomplete based on global word frequencies.
 */
public class GlobalFrequencyAutocompleteAlgorithm implements AutocompleteAlgorithm {

    /**
     * Human-readable label for the JavaFX ComboBox.
     */
    @Override
    public String name() {
        return "Global frequency (no context)";
    }

    /**
     * Suggest the most frequent words in the corpus, ignoring the last word.
     *
     * @param c        Open JDBC connection.
     * @param lastWord Last token typed by the user.  This parameter is ignored
     *                 by this algorithm; we keep it in the signature to honor
     *                 the AutocompleteAlgorithm contract.
     * @param limit    Maximum number of suggestions to return.  If {@code limit}
     *                 is less than or equal to zero, this returns an empty list.
     *
     * @return Up to {@code limit} words ordered by descending total_count.
     *
     * @throws Exception If a database error occurs.
     */
    @Override
    public List<String> suggest(Connection c, String lastWord, int limit) throws Exception {
        if (limit <= 0) {
            return List.of();
        }

        String sql =
                "SELECT word_text " +
                "FROM words " +
                "ORDER BY total_count DESC " +
                "LIMIT ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);

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

