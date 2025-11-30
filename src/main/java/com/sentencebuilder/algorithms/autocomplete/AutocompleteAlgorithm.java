/******************************************************************************
 * AutocompleteAlgorithm
 *
 * Strategy interface for all autocomplete engines used by the Sentence Builder
 * application.  Each implementation provides:
 *
 *   - A human-readable name to display in the UI.
 *   - A suggest(...) method that, given the current word/context and a limit,
 *     returns a ranked list of candidate next tokens.
 *
 * This abstraction lets the JavaFX UI expose multiple autocomplete strategies
 * (bigram-based, alliterative, global frequency, etc.) without knowing any of
 * the underlying SQL or probability logic.
 *
 * Written by <Your Name> (<Your NetID>) for CS 4485, Sentence Builder project,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.algorithms.autocomplete;

import java.sql.Connection;
import java.util.List;

/**
 * Common contract for autocomplete algorithms.  Implementations may use
 * different metadata (bigrams, trigrams, global frequencies, etc.), but
 * they all take the same inputs and produce a list of suggestions.
 */
public interface AutocompleteAlgorithm {

    /**
     * @return A short, human-readable name for this algorithm, suitable for
     *         display in the JavaFX ComboBox.
     */
    String name();

    /**
     * Suggest candidate next words based on the last word the user typed.
     *
     * Implementations are free to interpret {@code lastWord} however they
     * want, but in this project the UI passes the last token from the input
     * field (already split on whitespace).
     *
     * @param c        Open JDBC connection to the Sentence Builder database.
     * @param lastWord The last word (token) the user typed.  May be {@code null}
     *                 or blank, in which case implementations should generally
     *                 return an empty list.
     * @param limit    Maximum number of suggestions to return.  Implementations
     *                 should not return more than this many items.  Values
     *                 less than or equal to zero should result in an empty list.
     *
     * @return A list of candidate words, ordered from most to least preferred.
     *
     * @throws Exception If any database error or other checked exception occurs.
     */
    List<String> suggest(Connection c, String lastWord, int limit) throws Exception;
}
