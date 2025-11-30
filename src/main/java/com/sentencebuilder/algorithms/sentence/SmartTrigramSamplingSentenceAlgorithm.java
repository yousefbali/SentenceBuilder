/******************************************************************************
 * SmartTrigramSamplingSentenceAlgorithm
 *
 * Trigram-first, bigram-fallback sentence generator for the Sentence Builder
 * app.  This algorithm tries to balance three goals:
 *
 *   1. Use as much context as possible by preferring trigram transitions
 *      (w_{i-2}, w_{i-1} -> w_i) when available.
 *   2. Fall back to bigram transitions (w_{i-1} -> w_i) when no trigram data
 *      exists for the current context.
 *   3. Sample from the next-word distribution instead of greedily taking the
 *      single most common word, so different runs can yield different but
 *      still plausible sentences.
 *
 * Additionally, this algorithm tries to end sentences on words that frequently
 * appear at the end of sentences in the training corpus, using the per-word
 * sentence_end_count and total_count statistics.
 *
 * Written by <Your Name> (<Your NetID>) for CS 4485, Sentence Builder project,
 * starting <Month Day, 2025>.
 ******************************************************************************/
package com.sentencebuilder.algorithms.sentence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Implementation details and tuning parameters:
 *
 * - We cap candidate lists at 100 rows for both trigrams and bigrams to keep
 *   queries fast and memory usage small.
 * - Sampling uses a power-law temperature {@link #ALPHA} where ALPHA < 1.0
 *   flattens the distribution (more diversity) and ALPHA > 1.0 would sharpen
 *   it (more greedy).  Here we bias slightly toward diversity.
 * - We stop early once we have at least {@link #MIN_LENGTH_FOR_ENDING} words
 *   and the sampled word has an empirical sentence-end probability greater
 *   than {@link #END_THRESHOLD}.
 */
public class SmartTrigramSamplingSentenceAlgorithm implements SentenceAlgorithm {

    /** Exponent applied to frequencies when sampling; < 1.0 => smoother, more random. */
    private static final double ALPHA = 0.7;

    /** Minimum p(end | word) for us to consider a token a "good" sentence ending. */
    private static final double END_THRESHOLD = 0.35;

    /** Minimum sentence length before we even consider early stopping on an end word. */
    private static final int MIN_LENGTH_FOR_ENDING = 6;

    /** Random number generator used for all weighted sampling. */
    private final Random rng = new Random();

    /**
     * Human-readable name for this algorithm, shown in the JavaFX UI.
     *
     * @return A short label describing this strategy.
     */
    @Override
    public String name() {
        return "Smart Trigram (sample + endings)";
    }

    /**
     * Generate a sentence using trigram context where possible and bigrams as
     * a back-up.  The high-level flow is:
     *
     *   1. Normalize the starting word and store it as the first token.
     *   2. Sample a second word using bigrams so that we have enough context
     *      to begin using trigrams.
     *   3. Repeatedly:
     *        a. Try to sample from trigram statistics using the last two words.
     *        b. If no trigram exists for that context, fall back to bigrams
     *           based only on the last word.
     *        c. If still no candidates exist, stop.
     *        d. Otherwise, append the sampled word and, if appropriate, stop
     *           early when we hit a word that often ends sentences.
     *
     * @param c            Open JDBC connection to the Sentence Builder database.
     * @param startingWord The first word in the sentence, as chosen by the user.
     * @param maxWords     Maximum number of words (tokens) to generate, including
     *                     the starting word.
     *
     * @return A sentence beginning with {@code startingWord}.  If the starting
     *         word is blank, this returns {@code ""}.  If the corpus has no
     *         bigram continuation for the starting word, this returns the
     *         normalized starting word alone.
     *
     * @throws Exception If a database error occurs while looking up n-grams.
     */
    @Override
    public String generate(Connection c, String startingWord, int maxWords) throws Exception {
        if (startingWord == null || startingWord.isBlank()) {
            return "";
        }

        String seed = startingWord.trim().toLowerCase(Locale.ROOT);

        List<String> words = new ArrayList<>();
        words.add(seed);

        if (maxWords <= 1) {
            // Caller requested only the first word.
            return seed;
        }

        // Step 1: choose a second word using bigram sampling so that we can
        // start using trigram context from that point onward.
        Candidate second = sampleFromBigrams(c, seed);
        if (second == null) {
            // No known continuation after the seed; just return the seed.
            return seed;
        }
        words.add(second.word);

        // Step 2: main generation loop.  At each step we prefer trigram
        // statistics when available, with a bigram fallback.
        while (words.size() < maxWords) {
            String w1 = words.get(words.size() - 2);
            String w2 = words.get(words.size() - 1);

            Candidate next = sampleFromTrigrams(c, w1, w2);
            if (next == null) {
                // Fallback: if no trigram exists for (w1, w2), try bigrams
                // based solely on the last word.
                next = sampleFromBigrams(c, w2);
            }

            if (next == null) {
                // No outgoing n-grams at all for this context; end the sentence.
                break;
            }

            words.add(next.word);

            // Early stopping: if this word frequently appears at the end of
            // sentences and we've already reached a reasonable minimum length,
            // we allow the sentence to end here.
            if (words.size() >= MIN_LENGTH_FOR_ENDING &&
                next.endProbability >= END_THRESHOLD) {
                break;
            }
        }

        // Stitch the generated tokens back into a single space-separated string.
        StringBuilder sb = new StringBuilder();
        for (int ix = 0; ix < words.size(); ix++) {
            if (ix > 0) {
                sb.append(' ');
            }
            sb.append(words.get(ix));
        }
        return sb.toString();
    }

    // ---- helpers ---------------------------------------------------------

    /**
     * Sample a candidate using trigram statistics of the form
     * (prev2, prev1) -> next.  Results are weighted by frequency^ALPHA.
     *
     * @param c     Open JDBC connection.
     * @param prev2 Word at position i-2.
     * @param prev1 Word at position i-1.
     *
     * @return A sampled {@link Candidate}, or {@code null} if no trigram
     *         entries exist for this context.
     *
     * @throws Exception If a database error occurs.
     */
    private Candidate sampleFromTrigrams(Connection c, String prev2, String prev1) throws Exception {
        List<Candidate> candidates = new ArrayList<>();

        String sql = """
            SELECT w3.word_text, t.frequency, w3.sentence_end_count, w3.total_count
            FROM words w1
            JOIN word_relations_trigram t ON w1.word_id = t.first_word_id
            JOIN words w2 ON w2.word_id = t.second_word_id
            JOIN words w3 ON w3.word_id = t.next_word_id
            WHERE w1.word_text = ? AND w2.word_text = ?
            ORDER BY t.frequency DESC
            LIMIT 100
            """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prev2);
            ps.setString(2, prev1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nextWord = rs.getString(1);
                    int freq = rs.getInt(2);
                    int endCount = rs.getInt(3);
                    int totalCount = rs.getInt(4);
                    double endProb = totalCount > 0 ? (double) endCount / totalCount : 0.0;
                    candidates.add(new Candidate(nextWord, freq, endProb));
                }
            }
        }

        return weightedSample(candidates);
    }

    /**
     * Sample a candidate using bigram statistics of the form prev -> next.
     * Results are weighted by frequency^ALPHA.
     *
     * @param c    Open JDBC connection.
     * @param prev Word at position i-1.
     *
     * @return A sampled {@link Candidate}, or {@code null} if no bigram
     *         entries exist for this context.
     *
     * @throws Exception If a database error occurs.
     */
    private Candidate sampleFromBigrams(Connection c, String prev) throws Exception {
        List<Candidate> candidates = new ArrayList<>();

        String sql = """
            SELECT w2.word_text, r.frequency, w2.sentence_end_count, w2.total_count
            FROM words w1
            JOIN word_relations_bigram r ON w1.word_id = r.from_word_id
            JOIN words w2 ON w2.word_id = r.to_word_id
            WHERE w1.word_text = ?
            ORDER BY r.frequency DESC
            LIMIT 100
            """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prev);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nextWord = rs.getString(1);
                    int freq = rs.getInt(2);
                    int endCount = rs.getInt(3);
                    int totalCount = rs.getInt(4);
                    double endProb = totalCount > 0 ? (double) endCount / totalCount : 0.0;
                    candidates.add(new Candidate(nextWord, freq, endProb));
                }
            }
        }

        return weightedSample(candidates);
    }

    /**
     * Perform a single weighted random draw from a list of candidates using
     * the weights freq^ALPHA.  We intentionally re-compute the weights inside
     * this method instead of storing them in the {@link Candidate} to keep
     * that type simple; the candidate lists are capped at 100 elements so
     * this remains efficient.
     *
     * @param candidates List of possible next tokens.
     *
     * @return One sampled candidate, or {@code null} if the list is empty.
     */
    private Candidate weightedSample(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        double totalWeight = 0.0;
        for (Candidate cand : candidates) {
            totalWeight += Math.pow(cand.freq, ALPHA);
        }

        double r = rng.nextDouble() * totalWeight;
        double acc = 0.0;
        for (Candidate cand : candidates) {
            acc += Math.pow(cand.freq, ALPHA);
            if (r <= acc) {
                return cand;
            }
        }

        // In the extremely unlikely case of floating-point rounding error, fall
        // back to the last candidate so we always return *something*.
        return candidates.get(candidates.size() - 1);
    }

    /**
     * Small value class representing a potential next token in the sentence,
     * including both its n-gram frequency and its empirical probability of
     * appearing at the end of a sentence.
     */
    private static class Candidate {
        final String word;
        final int freq;
        final double endProbability;

        Candidate(String word, int freq, double endProbability) {
            this.word = word;
            this.freq = freq;
            this.endProbability = endProbability;
        }
    }
}
