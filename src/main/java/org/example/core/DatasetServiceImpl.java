package org.example.core;

import org.example.data.DatasetRepository;
import org.example.model.FileRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.util.List;

public class DatasetServiceImpl implements DatasetService {
    private final DatasetRepository repo;
    private final SuggestionService suggestions;

    public DatasetServiceImpl(DatasetRepository repo, SuggestionService suggestions) {
        this.repo = repo;
        this.suggestions = suggestions;
    }

    @Override
    public List<FileRecord> list() {
        return repo.list();
    }

    @Override
    public FileRecord importFile(File file) {
        int words = countWords(file);
        FileRecord fr = repo.insert(file.getName(), file.getAbsolutePath(), words, LocalDate.now());
        suggestions.rebuildModelAsync();
        return fr;
    }

    @Override
    public void remove(int id) {
        repo.delete(id);
        suggestions.rebuildModelAsync();
    }

    private int countWords(File f) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                for (String t : tokens) {
                    if (!t.isBlank()) count++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed reading file: " + f, e);
        }
        return count;
    }
}
