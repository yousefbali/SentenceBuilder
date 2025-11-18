
package com.sentencebuilder.tokenize;

import java.util.Map;

public class TokenizationResult {
    public record WordStats(int totalCount, int sentenceStartCount, int sentenceEndCount) {}
    public record Bigram(String from, String to) {}
    public record Trigram(String first, String second, String next) {}

    private final int wordCount;
    private final Map<String, WordStats> wordStats;
    private final Map<Bigram, Integer> bigrams;
    private final Map<Trigram, Integer> trigrams;

    public TokenizationResult(int wordCount,
                              Map<String, WordStats> wordStats,
                              Map<Bigram, Integer> bigrams,
                              Map<Trigram, Integer> trigrams) {
        this.wordCount = wordCount;
        this.wordStats = wordStats;
        this.bigrams = bigrams;
        this.trigrams = trigrams;
    }

    public int getWordCount() { return wordCount; }
    public Map<String, WordStats> getWordStats() { return wordStats; }
    public Map<Bigram, Integer> getBigrams() { return bigrams; }
    public Map<Trigram, Integer> getTrigrams() { return trigrams; }
}
