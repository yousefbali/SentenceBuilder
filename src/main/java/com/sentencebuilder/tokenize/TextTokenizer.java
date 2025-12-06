/******************************************************************************
 * TextTokenizer
 *
 * Interface for components that convert a plain text file into structured
 * token statistics for the Sentence Builder application.
 *
 * A TextTokenizer is responsible for:
 *   - Reading the file.
 *   - Breaking it into word tokens and sentence boundaries.
 *   - Counting word frequencies and n-gram relations.
 *   - Returning a TokenizationResult that can be written to the database.
 *
 * Written by Yonas Neguse (yxn220013) for <Course/Section>, Assignment <N>,
 * starting October 18, 2025>.
 ******************************************************************************/

package com.sentencebuilder.tokenize;

import java.io.File;

/**
 * Strategy interface for tokenizing a text file into word and n-gram statistics.
 */
public interface TextTokenizer {

    /**
     * Tokenize the given plain text file into word counts, bigrams, and trigrams.
     *
     * @param file input file to scan (assumed to be plain text).
     * @return TokenizationResult containing word and n-gram statistics.
     * @throws Exception if reading or tokenization fails for any reason.
     */
    TokenizationResult tokenize(File file) throws Exception;
}
