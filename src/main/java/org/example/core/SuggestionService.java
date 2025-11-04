package org.example.core;

import java.util.List;

public interface SuggestionService {
    List<String> suggestNext(String prefix, int k);
    void rebuildModelAsync();
}
