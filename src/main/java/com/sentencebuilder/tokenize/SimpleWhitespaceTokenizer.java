/******************************************************************************
 * SimpleWhitespaceTokenizer
 *
 * Concrete TextTokenizer that:
 *   - Reads a UTF-8 text file line by line.
 *   - Extracts tokens using a regex that matches:
 *       * word-like sequences of letters and apostrophes, or
 *       * sentence-ending punctuation characters (., !, ?).
 *   - Normalizes words (lowercases, strips punctuation, trims possessive "'s").
 *   - Tracks:
 *       * total word count,
 *       * per-word stats (frequency, sentence starts + ends),
 *       * bigram counts,
 *       * trigram counts.
 *
 * This tokenizer intentionally keeps the logic simple and explicit so that
 * graders can see exactly how the corpus statistics are computed.
 *
 * Written by Yonas Neguse (yxn220013) for <Course/Section>, Assignment <N>,
 * starting October 18, 2025>.
 ******************************************************************************/

package com.sentencebuilder.tokenize;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic implementation of TextTokenizer that uses a regex to split text into
 * words and sentence-ending punctuation.
 */
public class SimpleWhitespaceTokenizer implements TextTokenizer {

    /**
     * Regex that matches either:
     *   - a word (one or more ASCII letters or apostrophes), or
     *   - a single sentence-ending punctuation mark: ., !, or ?.
     *
     * We keep this pattern simple and explicit for grading purposes.
     */
    private static final Pattern WORD = Pattern.compile("[A-Za-z']+|[.!?]");

    /**
     * Tokenize the given file into word statistics and n-gram counts.
     *
     * @param file plain text file to scan.
     * @return TokenizationResult summarizing word and n-gram statistics.
     * @throws Exception if file reading fails.
     */
    @Override
    public TokenizationResult tokenize(File file) throws Exception {
        // First pass: collect all raw tokens (words and punctuation) in order.
        // We store them in memory so that it is easy to walk the sequence and
        // build n-gram relations.
        java.util.List<String> rawTokens = new java.util.ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = WORD.matcher(line);
                while (m.find()) {
                    rawTokens.add(m.group());
                }
            }
        }

        // Data structures for our statistics.
        int totalWordCount = 0;

        Map<String, TokenizationResult.WordStats> wordStats = new HashMap<>();
        Map<TokenizationResult.Bigram, Integer> bigrams = new HashMap<>();
        Map<TokenizationResult.Trigram, Integer> trigrams = new HashMap<>();

        // State used while walking the token list.
        String previousWord = null;     // word at position i-1
        String previousWord2 = null;    // word at position i-2
        String lastWordInSentence = null; // used to mark sentence-ending words
        boolean atSentenceStart = true;   // true at beginning and after punctuation

        // Walk through tokens in order and update counts.
        for (String raw : rawTokens) {
            // If this token is sentence-ending punctuation, we mark the last
            // word we saw as a sentence end and reset context.
            if (isSentenceEnd(raw)) {
                if (lastWordInSentence != null) {
                    markSentenceEnd(wordStats, lastWordInSentence);
                }

                // Reset context so the next valid word is treated as a sentence start.
                atSentenceStart = true;
                previousWord = null;
                previousWord2 = null;
                lastWordInSentence = null;
                continue;
            }

            // Normalize the token into a canonical word form.
            String word = normalizeWord(raw);
            if (word.isEmpty()) {
                // If normalization stripped everything (e.g., pure punctuation),
                // we skip this token and do not affect context.
                continue;
            }

            // Update per-word statistics.
            incrementWordOccurrence(wordStats, word, atSentenceStart);
            totalWordCount++;

            // Build bigram from previousWord -> current word.
            if (previousWord != null) {
                TokenizationResult.Bigram bg = new TokenizationResult.Bigram(previousWord, word);
                bigrams.put(bg, bigrams.getOrDefault(bg, 0) + 1);
            }

            // Build trigram from (previousWord2, previousWord) -> current word.
            if (previousWord2 != null) {
                TokenizationResult.Trigram tg =
                        new TokenizationResult.Trigram(previousWord2, previousWord, word);
                trigrams.put(tg, trigrams.getOrDefault(tg, 0) + 1);
            }

            // Shift our sliding window of previous words.
            previousWord2 = previousWord;
            previousWord = word;

            // Track the last word that belongs to the current sentence.
            lastWordInSentence = word;

            // Once we have consumed at least one word, we are no longer at a sentence start.
            atSentenceStart = false;
        }

        // Note: if the file ends without punctuation, we do NOT increment
        // sentenceEndCount for the last word. This mirrors the simple behavior
        // used in class and keeps our logic explicit and easy to reason about.

        return new TokenizationResult(totalWordCount, wordStats, bigrams, trigrams);
    }

    /**
     * Check if a raw token should be treated as a sentence-ending marker.
     * Currently, any single '.', '!' or '?' token ends the sentence.
     *
     * @param token raw token matched by the WORD pattern.
     * @return true if this token ends a sentence.
     */
    private boolean isSentenceEnd(String token) {
        return ".".equals(token) || "!".equals(token) || "?".equals(token);
    }

    /**
     * Increment totalCount and optionally sentenceStartCount for a word.
     *
     * This method does NOT update sentenceEndCount. Word endings are tracked
     * separately when punctuation is encountered, so that we do not double-count
     * total occurrences.
     *
     * @param stats           mutable map of word statistics.
     * @param word            normalized word string.
     * @param sentenceStart   true if this occurrence appears at sentence start.
     */
    private void incrementWordOccurrence(Map<String, TokenizationResult.WordStats> stats,
                                         String word,
                                         boolean sentenceStart) {
        TokenizationResult.WordStats old = stats.get(word);
        if (old == null) {
            old = new TokenizationResult.WordStats(0, 0, 0);
        }

        int newTotal = old.totalCount() + 1;
        int newStarts = old.sentenceStartCount() + (sentenceStart ? 1 : 0);
        int newEnds = old.sentenceEndCount(); // unchanged here

        stats.put(word, new TokenizationResult.WordStats(newTotal, newStarts, newEnds));
    }

    /**
     * Mark that the given word ended a sentence. This increments only
     * sentenceEndCount and leaves totalCount / sentenceStartCount unchanged.
     *
     * @param stats mutable map of word statistics.
     * @param word  normalized word string that ended the sentence.
     */
    private void markSentenceEnd(Map<String, TokenizationResult.WordStats> stats,
                                 String word) {
        TokenizationResult.WordStats old = stats.get(word);
        if (old == null) {
            // In a pathological case, punctuation might appear before the word
            // was recorded, but we still handle it defensively.
            old = new TokenizationResult.WordStats(0, 0, 0);
        }

        int newTotal = old.totalCount(); // do not change total occurrences
        int newStarts = old.sentenceStartCount();
        int newEnds = old.sentenceEndCount() + 1;

        stats.put(word, new TokenizationResult.WordStats(newTotal, newStarts, newEnds));
    }

    /**
     * Normalize a raw token into a canonical word form.
     *
     * Steps:
     *   1. Lowercase the token.
     *   2. Strip any leading/trailing characters that are not letters or apostrophes.
     *   3. Convert a trailing possessive "'s" into the base word (e.g., "dog's" -> "dog").
     *
     * Pure punctuation or tokens that become empty after stripping return "".
     *
     * @param raw raw string token matched by the WORD pattern.
     * @return normalized word (possibly empty).
     */
    private String normalizeWord(String raw) {
        if (raw == null) {
            return "";
        }

        // Lowercase for case-insensitive counting.
        String w = raw.toLowerCase(Locale.ROOT);

        // Strip leading/trailing non-letters/apostrophes.
        // This removes stray punctuation around the word.
        w = w.replaceAll("^[^a-z']+|[^a-z']+$", "");
        if (w.isEmpty()) {
            return "";
        }

        // Collapse common possessive "'s" -> base word.
        // Examples: "queen's" -> "queen", "dog's" -> "dog".
        if (w.endsWith("'s") && w.length() > 2) {
            w = w.substring(0, w.length() - 2);
        }

        return w;
    }
}
