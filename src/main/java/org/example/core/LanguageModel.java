package org.example.core;

import javafx.beans.property.ReadOnlyBooleanProperty;

import java.util.List;


/*
  Unified interface for language models.
 Extends SuggestionService and SentenceGenerator for flexibility.
  teammates can implement backend components here.
 */

public interface LanguageModel extends SuggestionService, SentenceGenerator {
    /** True while the model is rebuilding (background). */
    ReadOnlyBooleanProperty buildingProperty();
}
