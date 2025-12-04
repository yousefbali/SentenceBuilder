/******************************************************************************
 * TrigramGreedySentenceAlgorithm
 *
 * Greedy trigram-based sentence generator.
 *
 * Logic:
 *   - Interpret startingText as a sequence of words.
 *   - Cases:
 *       * 0 words:
 *           - Choose a good starting word (same logic as bigram greedy).
 *       * 1 word:
 *           - Use a bigram lookup to find the most likely second word.
 *       * 2+ words:
 *           - Use the LAST TWO words as trigram context (w1, w2).
 *
 *   - Once we have at least two words, at each step:
 *       1) Use the trigram table to find the most frequent "next" word
 *          given (w1, w2).
 *       2) Append that word and slide the window: (w1, w2) <- (w2, next).
 *
 *   - If a trigram continuation does not exist, we stop early.
 *   - We never exceed maxWords words in total.
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
 * Greedy sentence generator using the word_relations_trigram table,
 * falling back to bigrams to bootstrap the second word.
 */
public class TrigramGreedySentenceAlgorithm implements SentenceAlgorithm {

    @Override
    public String name() {
        return "Trigram – Greedy";
    }

    /**
     * Generate a sentence using trigram maximum-frequency continuation.
     */
    @Override
    public String generate(Connection c, String startingText, int maxWords) throws Exception {
        if (maxWords <= 0) {
            return "";
        }

        // Parse starting text into tokens.
        List<String> words = new ArrayList<>();
        if (startingText != null) {
            String trimmed = startingText.trim();
            if (!trimmed.isEmpty()) {
                words.addAll(Arrays.asList(trimmed.split("\\s+")));
            }
        }

        // Case 0: no words -> pick a starting word from the DB.
        if (words.isEmpty()) {
            String first = pickBestStartingWord(c);
            if (first == null) {
                return "";
            }
            words.add(first);
        }

        // If we only have a single word, try to get a "good" second word via bigrams.
        if (words.size() == 1 && words.size() < maxWords) {
            String second = getMostFrequentBigramSuccessor(c, words.get(0));
            if (second != null) {
                words.add(second);
            }
        }

        // Main loop: we now have at least 1 word; we try to use trigrams whenever we have 2.
        while (words.size() < maxWords) {
            if (words.size() < 2) {
                // Not enough context for trigram; we’re done.
                break;
            }

            String w1 = words.get(words.size() - 2);
            String w2 = words.get(words.size() - 1);

            String next = getMostFrequentTrigramSuccessor(c, w1, w2);
            if (next == null) {
                // No trigram continuation from (w1, w2); stop here.
                break;
            }

            words.add(next);
        }

        return String.join(" ", words);
    }

    /**
     * Pick a good starting word based on sentence_start_count, then total_count.
     * (Same logic as in BigramGreedySentenceAlgorithm.)
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
     * Bootstrap: given a single word, find a strong bigram successor to act
     * as the second word of the sentence.
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

    /**
     * Core trigram step: given (firstWord, secondWord), look up the most
     * frequent "next" word from the trigram table.
     *
     * Schema:
     *   - words(word_id, word_text, ...)
     *   - word_relations_trigram(first_word_id, second_word_id, next_word_id, frequency)
     */
    private String getMostFrequentTrigramSuccessor(Connection c,
                                                   String firstWord,
                                                   String secondWord) throws Exception {
        if (firstWord == null || secondWord == null ||
            firstWord.isBlank() || secondWord.isBlank()) {
            return null;
        }

        String w1 = firstWord.toLowerCase(Locale.ROOT);
        String w2 = secondWord.toLowerCase(Locale.ROOT);
/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
        String sql =
                "SELECT w3.word_text " +
                "FROM words w1 " +
                "JOIN word_relations_trigram tr ON w1.word_id = tr.first_word_id " +
                "JOIN words w2 ON tr.second_word_id = w2.word_id " +
                "JOIN words w3 ON tr.next_word_id = w3.word_id " +
                "WHERE w1.word_text = ? AND w2.word_text = ? " +
                "ORDER BY tr.frequency DESC, w3.word_text ASC " +
                "LIMIT 1";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, w1);
            ps.setString(2, w2);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }

        return null;
    }
}

