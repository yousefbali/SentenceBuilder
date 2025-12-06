/******************************************************************************
 * BigramRandomSentenceAlgorithm
 *
 * Sentence-generation strategy that follows the corpus bigram statistics but
 * uses random sampling instead of always picking the single most common next
 * word.  Given a starting word, it:
 *
 *   1. Normalizes the seed word to lower-case.
 *   2. At each step, queries the bigram table for the most frequent next words.
 *   3. Samples the next word with probability proportional to its bigram
 *      frequency, up to {@code maxWords} tokens total.
 *
 * This produces sentences that are faithful to the training text but still
 * varied across runs.
 *
 * Written by Yoel Kidane (yxk220039) for CS 4485, Sentence Builder project,
 * starting October 18, 2025.
 ******************************************************************************/
package com.sentencebuilder.algorithms.sentence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BigramRandomSentenceAlgorithm implements SentenceAlgorithm {

    /** Random number generator used for weighted bigram sampling. */
    private final Random rng = new Random();

    /**
     * Human-readable name for this algorithm, shown in the JavaFX UI.
     *
     * @return A short label describing this strategy.
     */
    @Override
    public String name() {
        return "Bigram Random Walk";
    }

    /**
     * Generate a sentence by walking the bigram graph using weighted random
     * selection at each step.
     *
     * @param c            Open JDBC connection to the Sentence Builder database.
     * @param startingWord The word chosen by the user to start the sentence.
     * @param maxWords     Maximum number of words in the generated sentence,
     *                     including the starting word.
     *
     * @return A bigram-based sentence starting with {@code startingWord}.  If
     *         {@code startingWord} is blank, this returns {@code ""}.  If the
     *         word has no outgoing bigrams in the corpus, this returns just the
     *         normalized starting word.
     *
     * @throws Exception If a database error occurs.
     */
    @Override
    public String generate(Connection c, String startingWord, int maxWords) throws Exception {
        if (startingWord == null || startingWord.isBlank()) {
            // Defensive: UI normally prevents this, but we handle it gracefully.
            return "";
        }

        // Normalize to the canonical form used in the DB.
        String current = startingWord.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(current);

        // We already have one word; extend up to maxWords by sampling bigrams.
        for (int wordIndex = 1; wordIndex < maxWords; wordIndex++) {
            List<Candidate> candidates = new ArrayList<>();

            // Look up the most frequent words that follow the current token.
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT w2.word_text, r.frequency " +
                    "FROM words w1 " +
                    "JOIN word_relations_bigram r ON w1.word_id = r.from_word_id " +
                    "JOIN words w2 ON w2.word_id = r.to_word_id " +
                    "WHERE w1.word_text = ? " +
                    "ORDER BY r.frequency DESC " +
                    "LIMIT 100")) {  // cap to keep queries fast and memory bounded
                ps.setString(1, current);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        candidates.add(new Candidate(
                                rs.getString(1),
                                rs.getInt(2)
                        ));
                    }
                }
            }

            if (candidates.isEmpty()) {
                // No outgoing bigrams for the current word; stop the walk.
                break;
            }

            // Compute the total mass of bigram frequencies for normalization.
            int totalFreq = 0;
            for (Candidate cnd : candidates) {
                totalFreq += cnd.freq;
            }

            // Draw a single word proportional to its bigram frequency.
            int r = rng.nextInt(totalFreq);
            String nextWord = null;
            int acc = 0;
            for (Candidate cnd : candidates) {
                acc += cnd.freq;
                if (r < acc) {
                    nextWord = cnd.word;
                    break;
                }
            }

            // This guard is mostly theoretical (defensive against arithmetic
            // issues); in practice, nextWord should always be non-null here.
            if (nextWord == null) {
                break;
            }

            sb.append(' ').append(nextWord);
            current = nextWord.toLowerCase(Locale.ROOT);
        }

        return sb.toString();
    }

    /**
     * Small value class that couples a candidate next word with its bigram
     * frequency.  We keep this as a nested type because it is only used by
     * this algorithm.
     */
    private static class Candidate {
        final String word;
        final int freq;

        Candidate(String word, int freq) {
            this.word = word;
            this.freq = freq;
        }
    }
}

