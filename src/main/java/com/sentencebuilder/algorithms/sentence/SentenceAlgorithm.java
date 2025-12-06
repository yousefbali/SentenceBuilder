/******************************************************************************
 * SentenceAlgorithm
 *
 * Common interface for all sentence-generation strategies.
 *
 * Each implementation:
 *   - Exposes a human-readable name() for use in the UI.
 *   - Implements generate(...) given a JDBC Connection and a starting text.
 *
 * Implementations are responsible for:
 *   - Interpreting the starting text (single word or phrase).
 *   - Respecting the maxWords upper bound.
 *   - Handling empty/invalid starting text gracefully by choosing a reasonable
 *     starting point from the database if needed.
 *
 * Written by Yoel Kidane (yxk220039) for CS 4485, Sentence Builder project,
 * starting October 9, 2025.
 ******************************************************************************/

package com.sentencebuilder.algorithms.sentence;

import java.sql.Connection;

/**
 * Strategy interface for sentence generation algorithms.
 */
public interface SentenceAlgorithm {

    /**
     * @return human-readable name of the algorithm (for combo boxes, etc.).
     */
    String name();

    /**
     * Generate a sentence using this algorithm.
     *
     * @param c            open JDBC connection to the sentencegen database.
     * @param startingText seed text typed by the user (may be empty or null).
     *                     Implementations decide how to interpret this:
     *                     - single word
     *                     - multi-word phrase (using last one/two words as context)
     * @param maxWords     hard cap on the number of words in the result.
     * @return generated sentence text, or an empty string if generation fails.
     * @throws Exception if a database or logic error prevents generation.
     */
    String generate(Connection c, String startingText, int maxWords) throws Exception;
}
