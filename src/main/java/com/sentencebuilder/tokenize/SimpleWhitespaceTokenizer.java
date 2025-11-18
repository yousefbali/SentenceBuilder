
package com.sentencebuilder.tokenize;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;

public class SimpleWhitespaceTokenizer implements TextTokenizer {
    private static final Pattern WORD = Pattern.compile("[A-Za-z']+|[.!?]");

    @Override
    public TokenizationResult tokenize(File file) throws Exception {
        List<String> tokens = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = WORD.matcher(line);
                while (m.find()) tokens.add(m.group().toLowerCase(Locale.ROOT));
            }
        }

        Map<String, TokenizationResult.WordStats> wordStats = new HashMap<>();
        Map<TokenizationResult.Bigram, Integer> bigrams = new HashMap<>();
        Map<TokenizationResult.Trigram, Integer> trigrams = new HashMap<>();

        String prev = null;
        String prev2 = null;
        boolean sentenceStart = true;

        for (int i = 0; i < tokens.size(); i++) {
            String tok = tokens.get(i);
            if (tok.matches("[.!?]")) {
                if (prev != null) {
                    TokenizationResult.WordStats ws = wordStats.getOrDefault(prev, new TokenizationResult.WordStats(0,0,0));
                    wordStats.put(prev, new TokenizationResult.WordStats(ws.totalCount(), ws.sentenceStartCount(), ws.sentenceEndCount()+1));
                }
                sentenceStart = true;
                prev2 = null;
                prev = null;
                continue;
            }

            TokenizationResult.WordStats ws = wordStats.getOrDefault(tok, new TokenizationResult.WordStats(0,0,0));
            ws = new TokenizationResult.WordStats(ws.totalCount()+1, ws.sentenceStartCount() + (sentenceStart?1:0), ws.sentenceEndCount());
            wordStats.put(tok, ws);

            if (prev != null) {
                TokenizationResult.Bigram b = new TokenizationResult.Bigram(prev, tok);
                bigrams.put(b, bigrams.getOrDefault(b, 0)+1);
            }
            if (prev2 != null) {
                TokenizationResult.Trigram t = new TokenizationResult.Trigram(prev2, prev, tok);
                trigrams.put(t, trigrams.getOrDefault(t, 0)+1);
            }

            sentenceStart = false;
            prev2 = prev;
            prev = tok;
        }

        return new TokenizationResult(tokens.size(), wordStats, bigrams, trigrams);
    }
}
