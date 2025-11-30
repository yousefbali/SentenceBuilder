/******************************************************************************
 * TokenizationResult
 *
 * Immutable container for the output of a TextTokenizer. It captures:
 *   - The total number of word tokens observed.
 *   - Per-word statistics (frequency, sentence-start count, sentence-end count).
 *   - Bigram counts (word_i, word_{i+1}).
 *   - Trigram counts (word_i, word_{i+1}, word_{i+2}).
 *
 * This object is later used by the import service to write data into the
 * Sentence Builder database schema.
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.tokenize;

import java.util.Map;

/**
 * Immutable data transfer object representing all token stats from a file.
 */
public class TokenizationResult {

    /**
     * Per-word statistics:
     *   - totalCount: how many times this word appears in the corpus.
     *   - sentenceStartCount: how many times this word starts a sentence.
     *   - sentenceEndCount: how many times this word ends a sentence.
     */
    public record WordStats(int totalCount, int sentenceStartCount, int sentenceEndCount) {}

    /**
     * Bigram key: a directed pair (from -> to) representing two neighboring
     * words in the token stream.
     */
    public record Bigram(String from, String to) {}

    /**
     * Trigram key: a directed triple (first, second -> next) representing
     * three consecutive words.
     */
    public record Trigram(String first, String second, String next) {}

    /** Total number of word tokens observed (after normalization). */
    private final int wordCount;

    /** Per-word statistics for the entire corpus. */
    private final Map<String, WordStats> wordStats;

    /** Counts for each observed bigram (from, to). */
    private final Map<Bigram, Integer> bigrams;

    /** Counts for each observed trigram (first, second, next). */
    private final Map<Trigram, Integer> trigrams;

    /**
     * Build a new immutable TokenizationResult.
     *
     * @param wordCount total number of word tokens.
     * @param wordStats per-word statistics.
     * @param bigrams   bigram counts.
     * @param trigrams  trigram counts.
     */
    public TokenizationResult(int wordCount,
                              Map<String, WordStats> wordStats,
                              Map<Bigram, Integer> bigrams,
                              Map<Trigram, Integer> trigrams) {
        this.wordCount = wordCount;
        this.wordStats = wordStats;
        this.bigrams = bigrams;
        this.trigrams = trigrams;
    }

    /**
     * @return total number of word tokens observed.
     */
    public int getWordCount() {
        return wordCount;
    }

    /**
     * @return mutable map of per-word statistics.
     *         Keys are normalized word strings.
     */
    public Map<String, WordStats> getWordStats() {
        return wordStats;
    }

    /**
     * @return mutable map of bigram counts. Keys are (from, to) pairs.
     */
    public Map<Bigram, Integer> getBigrams() {
        return bigrams;
    }

    /**
     * @return mutable map of trigram counts. Keys are (first, second, next) triples.
     */
    public Map<Trigram, Integer> getTrigrams() {
        return trigrams;
    }
}
