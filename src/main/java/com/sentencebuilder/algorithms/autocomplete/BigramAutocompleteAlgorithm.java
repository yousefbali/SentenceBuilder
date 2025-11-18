
package com.sentencebuilder.algorithms.autocomplete;

import java.sql.*;
import java.util.*;

public class BigramAutocompleteAlgorithm implements AutocompleteAlgorithm {
    @Override
    public String name() { return "Bigram Top-N"; }

    @Override
    public List<String> suggest(Connection c, String lastWord, int limit) throws Exception {
        if (lastWord == null || lastWord.isBlank()) return List.of();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT w2.word_text, r.frequency " +
                "FROM words w1 " +
                "JOIN word_relations_bigram r ON r.from_word_id = w1.word_id " +
                "JOIN words w2 ON w2.word_id = r.to_word_id " +
                "WHERE w1.word_text = ? " +
                "ORDER BY r.frequency DESC " +
                "LIMIT ?")) {
            ps.setString(1, lastWord.toLowerCase(Locale.ROOT));
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        }
    }
}
