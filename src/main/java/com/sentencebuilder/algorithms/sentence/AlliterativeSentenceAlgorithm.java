/******************************************************************************
 * AlliterativeSentenceAlgorithm
 *
 * Sentence-generation strategy for the Sentence Builder app that produces
 * playful, "alliterative" sentences.  Given a starting word, it:
 *
 *   1. Normalizes the seed word to lower-case.
 *   2. Finds other words in the corpus that start with the same first letter.
 *   3. Randomly samples from that set (with equal probability) to extend
 *      the sentence up to the requested maximum length.
 *
 * This algorithm deliberately ignores bigram/trigram structure and focuses
 * only on first-letter alliteration.  It is useful as a contrast to the more
 * statistically grounded n-gram algorithms.
 *
 * Written by Ali Saidane (axs220579) for CS 4485, Sentence Builder project,
 * starting November 11, 2025.
 ******************************************************************************/
package com.sentencebuilder.algorithms.sentence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class AlliterativeSentenceAlgorithm implements SentenceAlgorithm {

    /** Random number generator used for sampling alliterative words. */
    private final Random rng = new Random();

    /**
     * Human-readable name for this algorithm, shown in the JavaFX UI.
     *
     * @return A short label describing this strategy.
     */
    @Override
    public String name() {
        return "Alliterative (same first letter)";
    }

    /**
     * Generate an alliterative sentence.
     *
     * @param c            Open JDBC connection to the Sentence Builder database.
     * @param startingWord The word the user chose to start the sentence.
     *                     Only the first character is used for alliteration.
     * @param maxWords     Maximum number of words in the returned sentence,
     *                     including the starting word.
     *
     * @return A sentence that begins with {@code startingWord} and continues
     *         with randomly chosen words that share the same first letter.
     *         If the starting word is blank, this returns {@code ""}.  If no
     *         other alliterative words exist in the corpus, this returns the
     *         normalized starting word unchanged.
     *
     * @throws Exception If a database error occurs.
     */
    @Override
    public String generate(Connection c, String startingWord, int maxWords) throws Exception {
        if (startingWord == null || startingWord.isBlank()) {
            // Nothing to build from; the UI already discourages this, but we
            // guard it anyway for robustness.
            return "";
        }

        // Normalize to lower case so we match the way tokens are stored in DB.
        String seed = startingWord.trim().toLowerCase(Locale.ROOT);
        char first = seed.charAt(0);
        if (!Character.isLetter(first)) {
            // If the "word" starts with punctuation or a digit, there is no
            // meaningful alliterative cohort; just echo the seed back.
            return seed;
        }

        // Collect candidate words that start with the same first letter, ordered
        // by total frequency so more common words appear more often in the pool.
        List<String> candidates = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT word_text " +
                "FROM words " +
                "WHERE word_text LIKE ? " +
                "ORDER BY total_count DESC " +
                "LIMIT 200")) { // cap list size to keep memory and latency bounded
            ps.setString(1, first + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String w = rs.getString(1);
                    // Avoid immediately repeating the seed; later samples are
                    // allowed to repeat naturally.
                    if (!w.equals(seed)) {
                        candidates.add(w);
                    }
                }
            }
        }

        // If no alliterative neighbors exist, just return the cleaned-up seed.
        if (candidates.isEmpty()) {
            return seed;
        }

        // Seed is the first token in the sentence.
        StringBuilder sb = new StringBuilder(seed);
        int remaining = Math.max(0, maxWords - 1);

        // Repeatedly sample uniformly from the alliterative candidate list.
        for (int ix = 0; ix < remaining; ix++) {
            String next = candidates.get(rng.nextInt(candidates.size()));
            sb.append(' ').append(next);
        }

        return sb.toString();
    }
}

