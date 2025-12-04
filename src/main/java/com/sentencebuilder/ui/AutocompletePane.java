/******************************************************************************
 * AutocompletePane
 *
 * UI pane for the Sentence Builder application that lets the user type a partial
 * sentence and see suggested next words from different autocomplete algorithms
 * (bigram, trigram-with-context, alliterative, global frequency, etc.).
 *
 * The pane:
 *   - Builds the JavaFX controls for settings, input, and suggestions.
 *   - Lets the user pick an AutocompleteAlgorithm implementation.
 *   - Listens for changes to the text field, algorithm selection, and Top-N limit.
 *   - Fetches suggestions from the database using the selected algorithm.
 *   - Allows the user to accept a suggestion with a mouse click or TAB key.
 *
 * Written by Yousef Ali  for CS 4485.0W1, 
 * starting October 23, 2025.
 ******************************************************************************/

package com.sentencebuilder.ui;

import com.sentencebuilder.algorithms.autocomplete.AlliterativeAutocompleteAlgorithm;
import com.sentencebuilder.algorithms.autocomplete.AutocompleteAlgorithm;
import com.sentencebuilder.algorithms.autocomplete.BigramAutocompleteAlgorithm;
import com.sentencebuilder.algorithms.autocomplete.ContextTrigramAutocompleteAlgorithm;
import com.sentencebuilder.algorithms.autocomplete.GlobalFrequencyAutocompleteAlgorithm;
import com.sentencebuilder.dao.Database;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX pane that encapsulates the autocomplete UI.
 * This class is responsible only for building the controls and wiring them
 * to the AutocompleteAlgorithm strategy objects and the database.
 */
public class AutocompletePane extends BorderPane {

    /** Drop-down to select which autocomplete algorithm to use. */
    private final ComboBox<AutocompleteAlgorithm> algoPicker = new ComboBox<>();

    /** Text field where the user types the partial sentence. */
    private final TextField input = new TextField();

    /** List of suggested next words based on the current algorithm and input. */
    private final ListView<String> suggestions = new ListView<>();

    /**
     * Spinner controlling how many suggestions (Top-N) are requested from
     * the autocomplete algorithm for each query.
     */
    private final Spinner<Integer> limit = new Spinner<>(1, 20, 5);

    /**
     * Construct the autocomplete pane.
     *
     * This constructor:
     *   - Applies high-level styling to match the rest of the app.
     *   - Creates the header (title + subtitle).
     *   - Creates the settings row (algorithm picker + Top-N spinner).
     *   - Creates the input + suggestions section.
     *   - Wires up event handlers so suggestions update as the user types.
     */
    public AutocompletePane() {
        // Page background
        getStyleClass().add("page-root");

        // Header
        Label title = new Label("Autocomplete");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label(
                "Type a partial sentence and let the n-gram models suggest the next word."
        );
        subtitle.getStyleClass().add("page-subtitle");

        VBox headerBox = new VBox(4, title, subtitle);

        // Controls row (settings)
        Label controlsLabel = new Label("Settings");
        controlsLabel.getStyleClass().add("section-title");

        limit.setEditable(true);
        HBox.setHgrow(input, Priority.ALWAYS);

        HBox controlRow = new HBox(
                8,
                new Label("Algorithm:"), algoPicker,
                new Label("Top-N:"), limit
        );
        controlRow.getStyleClass().add("toolbar-row");

        // Input + suggestions area
        Label inputLabel = new Label("Type below");
        inputLabel.getStyleClass().add("section-title");

        Label suggestionsLabel = new Label("Suggestions");
        suggestionsLabel.getStyleClass().add("section-title");

        VBox.setVgrow(suggestions, Priority.ALWAYS);

        VBox body = new VBox(
                10,
                headerBox,
                new Separator(),
                controlsLabel,
                controlRow,
                new Separator(),
                inputLabel,
                input,
                suggestionsLabel,
                suggestions
        );

        // Outer card layout
        BorderPane card = new BorderPane(body);
        card.getStyleClass().addAll("page-card", "page-card-wide");
        card.setPadding(new Insets(0));

        setCenter(card);
        BorderPane.setMargin(card, new Insets(0, 0, 16, 0));

        // Algorithm wiring
        List<AutocompleteAlgorithm> algorithms = new ArrayList<>();
        algorithms.add(new BigramAutocompleteAlgorithm());
        algorithms.add(new AlliterativeAutocompleteAlgorithm());
        algorithms.add(new GlobalFrequencyAutocompleteAlgorithm());
        algorithms.add(new ContextTrigramAutocompleteAlgorithm());
        // Additional algorithms can be added here in the future.

        algoPicker.getItems().addAll(algorithms);

        // Show the user-friendly name() of each algorithm in the combo box.
        algoPicker.setCellFactory(comboBox -> new ListCell<>() {
            @Override
            protected void updateItem(AutocompleteAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        algoPicker.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AutocompleteAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        algoPicker.getSelectionModel().selectFirst();

        // Behavior / event wiring

        // Whenever the text changes, recompute suggestions.
        input.textProperty().addListener((obs, oldValue, newValue) -> updateSuggestions());

        // Recompute suggestions when Top-N changes or a different algorithm is chosen.
        limit.valueProperty().addListener((obs, oldValue, newValue) -> updateSuggestions());
        algoPicker.valueProperty().addListener((obs, oldValue, newValue) -> updateSuggestions());

        // Allow clicking on a suggestion to append it to the input field.
        suggestions.setOnMouseClicked(event -> {
            String selected = suggestions.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.startsWith("Error:")) {
                appendWord(selected);
            }
        });

        // Allow pressing TAB to accept the first suggestion (if any).
        input.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB && !suggestions.getItems().isEmpty()) {
                String first = suggestions.getItems().get(0);
                if (!first.startsWith("Error:")) {
                    appendWord(first);
                    event.consume();   // Prevent default TAB focus traversal.
                }
            }
        });
    }

    /**
     * Recompute the list of suggested words based on:
     *   - the current contents of the input TextField,
     *   - the currently selected autocomplete algorithm,
     *   - and the current Top-N value in the Spinner.
     *
     * This method performs a database query through the AutocompleteAlgorithm.
     * For our dataset sizes, this runs synchronously on the JavaFX thread.
     */
    private void updateSuggestions() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            suggestions.setItems(FXCollections.observableArrayList());
            return;
        }

        AutocompleteAlgorithm algo = algoPicker.getValue();
        if (algo == null) {
            suggestions.setItems(FXCollections.observableArrayList());
            return;
        }

        // Extract the last word typed so we can decide how to query.
        String[] pieces = text.split("\\s+");
        String last = pieces[pieces.length - 1];

        // Some algorithms (like ContextTrigramAutocompleteAlgorithm) require the
        // full context string, while simpler models only need the last word.
        String queryArgument;
        if (algo instanceof ContextTrigramAutocompleteAlgorithm) {
            queryArgument = text;
        } else {
            queryArgument = last;
        }

        try (Connection conn = Database.get()) {
            List<String> results = algo.suggest(conn, queryArgument, limit.getValue());
            suggestions.setItems(FXCollections.observableArrayList(results));
        } catch (Exception ex) {
            suggestions.setItems(FXCollections.observableArrayList("Error: " + ex.getMessage()));
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load autocomplete suggestions:\n" + ex.getMessage()
            ).showAndWait();
        }
    }

    /**
     * Append the given word to the end of the input TextField.
     * This is called when the user accepts a suggestion with the mouse or TAB.
     *
     * @param word the word to append to the current text
     */
    private void appendWord(String word) {
        String existing = input.getText().trim();

        if (existing.isEmpty()) {
            input.setText(word);
        } else {
            input.setText(existing + " " + word);
        }

        // Move the caret to the end so the user can keep typing naturally.
        input.positionCaret(input.getText().length());

        // Immediately refresh suggestions based on the new text.
        updateSuggestions();
    }
}

