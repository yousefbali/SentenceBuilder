
package com.sentencebuilder.ui;

import com.sentencebuilder.algorithms.autocomplete.AutocompleteAlgorithm;
import com.sentencebuilder.algorithms.autocomplete.BigramAutocompleteAlgorithm;
import com.sentencebuilder.dao.Database;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class AutocompletePane extends BorderPane {
    private final ComboBox<AutocompleteAlgorithm> algoPicker = new ComboBox<>();
    private final TextField input = new TextField();
    private final ListView<String> suggestions = new ListView<>();
    private final Spinner<Integer> limit = new Spinner<>(1, 20, 5);

    public AutocompletePane() {
        setPadding(new Insets(15));
        HBox top = new HBox(10, new Label("Algorithm:"), algoPicker, new Label("Top-N:"), limit);
        setTop(top);

        VBox center = new VBox(8, new Label("Type below:"), input, new Label("Suggestions:"), suggestions);
        setCenter(center);

        List<AutocompleteAlgorithm> algos = new ArrayList<>();
        algos.add(new BigramAutocompleteAlgorithm());
        // add algos here
        
        algoPicker.getItems().addAll(algos);
        algoPicker.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(AutocompleteAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        algoPicker.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(AutocompleteAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        algoPicker.getSelectionModel().selectFirst();

        input.textProperty().addListener((obs, old, val) -> updateSuggestions());

        suggestions.setOnMouseClicked(e -> {
            String sel = suggestions.getSelectionModel().getSelectedItem();
            if (sel != null) appendWord(sel);
        });
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.TAB && !suggestions.getItems().isEmpty()) {
                appendWord(suggestions.getItems().get(0));
                e.consume();
            }
        });
    }

    private void updateSuggestions() {
        String[] parts = input.getText().trim().split("\\s+");
        String last = parts.length == 0 ? "" : parts[parts.length-1];
        try (Connection c = Database.get()) {
            List<String> s = algoPicker.getValue().suggest(c, last, limit.getValue());
            suggestions.setItems(FXCollections.observableArrayList(s));
        } catch (Exception ex) {
            suggestions.setItems(FXCollections.observableArrayList("Error: " + ex.getMessage()));
        }
    }

    private void appendWord(String w) {
        String t = input.getText().trim();
        if (t.isEmpty()) input.setText(w);
        else input.setText(t + " " + w);
        input.positionCaret(input.getText().length());
        updateSuggestions();
    }
}
