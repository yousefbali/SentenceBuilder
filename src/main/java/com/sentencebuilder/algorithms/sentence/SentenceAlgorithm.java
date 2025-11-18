
package com.sentencebuilder.algorithms.sentence;

import java.sql.Connection;

public interface SentenceAlgorithm {
    String name();
    String generate(Connection c, String startingWord, int maxWords) throws Exception;
}
