
package com.sentencebuilder.tokenize;

import java.io.File;

public interface TextTokenizer {
    TokenizationResult tokenize(File file) throws Exception;
}
