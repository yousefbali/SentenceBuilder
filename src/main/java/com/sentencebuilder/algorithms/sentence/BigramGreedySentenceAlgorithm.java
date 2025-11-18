
package com.sentencebuilder.algorithms.sentence;

import java.sql.*;
import java.util.Locale;

public class BigramGreedySentenceAlgorithm implements SentenceAlgorithm {

    @Override
    public String name() { return "Bigram Greedy"; }

    @Override
    public String generate(Connection c, String startingWord, int maxWords) throws Exception {
        StringBuilder sb = new StringBuilder();
        String current = startingWord.toLowerCase(Locale.ROOT);
        sb.append(current);

        for (int i = 1; i < maxWords; i++) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT w2.word_text, r.frequency " +
                    "FROM words w1 " +
                    "JOIN word_relations_bigram r ON r.from_word_id = w1.word_id " +
                    "JOIN words w2 ON w2.word_id = r.to_word_id " +
                    "WHERE w1.word_text = ? " +
                    "ORDER BY r.frequency DESC " +
                    "LIMIT 1")) {
                ps.setString(1, current);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) break;
                    current = rs.getString(1);
                    sb.append(" ").append(current);
                }
            }
        }
        return sb.toString();
    }
}
