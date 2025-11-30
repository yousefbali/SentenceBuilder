
package com.sentencebuilder.ui;

import com.sentencebuilder.algorithms.sentence.BigramGreedySentenceAlgorithm;
import com.sentencebuilder.algorithms.sentence.SentenceAlgorithm;
import com.sentencebuilder.algorithms.sentence.TrigramGreedySentenceAlgorithm;
import com.sentencebuilder.dao.Database;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class SentenceGeneratorPane extends BorderPane {
    private final ComboBox<SentenceAlgorithm> algoPicker = new ComboBox<>();
    private final TextField startWord = new TextField();
    private final Spinner<Integer> maxWords = new Spinner<>(5, 100, 20);
    private final Button generateBtn = new Button("Generate");
    private final TextArea output = new TextArea();

    public SentenceGeneratorPane() {
        getStyleClass().add("content-pane");
        setPadding(new Insets(0));

        Label title = new Label("Sentence Generator");
        title.getStyleClass().add("section-title");

        HBox controls = new HBox(10,
            new Label("Algorithm:"), algoPicker,
            new Label("Start:"), startWord,
            new Label("Max words:"), maxWords,
            generateBtn
        );
        VBox topCard = new VBox(8, title, controls);
        topCard.getStyleClass().add("card");
        setTop(topCard);

        output.setPrefRowCount(18);
        output.setWrapText(true);
        output.getStyleClass().add("sub-card");
        setCenter(output);

        generateBtn.getStyleClass().add("btn-primary");

        List<SentenceAlgorithm> algos = new ArrayList<>();
        algos.add(new BigramGreedySentenceAlgorithm());
        algos.add(new TrigramGreedySentenceAlgorithm());
        // add algos here

        algoPicker.getItems().addAll(algos);
        algoPicker.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(SentenceAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        algoPicker.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(SentenceAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        algoPicker.getSelectionModel().selectFirst();

        generateBtn.setOnAction(e -> generate());
    }

    private void generate() {
        SentenceAlgorithm algo = algoPicker.getValue();
        if (algo == null) return;
        if (startWord.getText().isBlank()) {
            output.setText("Please provide a starting word.");
            return;
        }
        try (Connection c = Database.get()) {
            String s = algo.generate(c, startWord.getText().trim(), maxWords.getValue());
            output.setText(s);
        } catch (Exception ex) {
            output.setText("Error: " + ex.getMessage());
        }
    }
}
