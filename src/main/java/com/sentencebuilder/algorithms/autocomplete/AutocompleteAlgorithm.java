
package com.sentencebuilder.algorithms.autocomplete;

import java.sql.Connection;
import java.util.List;

public interface AutocompleteAlgorithm {
    String name();
    List<String> suggest(Connection c, String lastWord, int limit) throws Exception;
}
