package org.example.core;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;
import org.example.data.DatasetRepository;
import org.example.model.FileRecord;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


/**
 Basic N-gram language model implementation.
 Builds simple bigrams from dataset files stored in DatasetRepository.
 
  TODO: Teammates can replace this class with a more advanced tokenizer
  or ML model by implementing the LanguageModel interface.
 */

public class NGramModel implements LanguageModel {

    private final DatasetRepository repo;
    private final Map<String, Map<String, Integer>> bigrams = new ConcurrentHashMap<>();
    private final Pattern tokenSplit = Pattern.compile("[^a-zA-Z0-9']+");
    private final ReadOnlyBooleanWrapper building = new ReadOnlyBooleanWrapper(false);

    public NGramModel(DatasetRepository repo) {
        this.repo = repo;
        rebuildModelAsync();
    }

    @Override
    public List<String> suggestNext(String prefix, int k) {
        if (prefix == null || prefix.isBlank()) return List.of();
        String last = lastToken(prefix.toLowerCase());
        if (last.isBlank()) return List.of();
        Map<String, Integer> nexts = bigrams.getOrDefault(last, Collections.emptyMap());
        return nexts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(Math.max(0, k))
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public void rebuildModelAsync() {
        if (building.get()) return;
        building.set(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Map<String, Map<String, Integer>> tmp = new HashMap<>();

                for (FileRecord fr : repo.list()) {
                    Path p = Path.of(fr.path());
                    if (!Files.isRegularFile(p)) continue;

                    try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                        String line;
                        String prev = null;
                        while ((line = br.readLine()) != null) {
                            for (String tok : tokenize(line)) {
                                if (prev != null) {
                                    tmp.computeIfAbsent(prev, k -> new HashMap<>())
                                       .merge(tok, 1, Integer::sum);
                                }
                                prev = tok;
                            }
                            prev = null; // reset at end of line
                        }
                    } catch (Exception ignored) {
                        // skip bad files rather than fail the whole build
                    }
                }

                bigrams.clear();
                tmp.forEach((k, v) -> bigrams.put(k, new ConcurrentHashMap<>(v)));
                building.set(false);
                return null;
            }
        };

        Thread t = new Thread(task, "ngram-build");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public String generate(String prompt, int maxWords) {
        String base = prompt == null ? "" : prompt.trim();
        StringBuilder sb = new StringBuilder(base);
        String last = lastToken(base.toLowerCase());
        if (last.isBlank()) return sb.toString();

        int limit = Math.max(1, maxWords);
        for (int i = 0; i < limit; i++) {
            String next = pickTop(last);
            if (next == null) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(next);
            last = next;
        }
        return sb.toString();
    }

    @Override
    public ReadOnlyBooleanProperty buildingProperty() {
        return building.getReadOnlyProperty();
    }

    // Helpers

    private String pickTop(String token) {
        Map<String, Integer> m = bigrams.get(token);
        if (m == null || m.isEmpty()) return null;
        return m.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<String> tokenize(String s) {
        return Arrays.stream(tokenSplit.split(s.toLowerCase()))
                .filter(t -> !t.isBlank())
                .toList();
    }

    private String lastToken(String s) {
        String[] parts = tokenSplit.split(s == null ? "" : s);
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }
}
