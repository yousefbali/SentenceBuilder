/******************************************************************************
 * SentenceGeneratorPane
 *
 * UI pane that lets the user generate sentences from the imported corpus
 * using different n-gram-based sentence algorithms (bigram, trigram, random,
 * alliterative, smart trigram sampling, etc.).
 *
 * The pane:
 *   - Lets the user pick a SentenceAlgorithm strategy.
 *   - Accepts a starting word and maximum number of words.
 *   - Calls the algorithm with a database connection to generate a sentence.
 *   - Displays the generated sentence in a read-only TextArea.
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.ui;

import com.sentencebuilder.algorithms.sentence.AlliterativeSentenceAlgorithm;
import com.sentencebuilder.algorithms.sentence.BigramGreedySentenceAlgorithm;
import com.sentencebuilder.algorithms.sentence.BigramRandomSentenceAlgorithm;
import com.sentencebuilder.algorithms.sentence.SentenceAlgorithm;
import com.sentencebuilder.algorithms.sentence.SmartTrigramSamplingSentenceAlgorithm;
import com.sentencebuilder.algorithms.sentence.TrigramGreedySentenceAlgorithm;
import com.sentencebuilder.dao.Database;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX pane for configuring and running sentence generation algorithms.
 */
public class SentenceGeneratorPane extends BorderPane {

    /** Drop-down for selecting which sentence algorithm to use. */
    private final javafx.scene.control.ComboBox<SentenceAlgorithm> algoPicker =
            new javafx.scene.control.ComboBox<>();

    /** Starting word for the sentence. */
    private final TextField startWord = new TextField();

    /** Maximum number of words the algorithm is allowed to generate. */
    private final Spinner<Integer> maxWords = new Spinner<>(1, 100, 15);

    /** Read-only output area where the generated sentence is displayed. */
    private final TextArea output = new TextArea();

    /** Button that triggers sentence generation. */
    private final Button generateBtn = new Button("Generate");

    /**
     * Construct the sentence generator pane and wire up all controls.
     */
    public SentenceGeneratorPane() {
        // Page background
        getStyleClass().add("page-root");

        // Header
        Label title = new Label("Sentence generator");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label(
                "Pick a model, choose a starting word, and let the n-gram generator complete the sentence."
        );
        subtitle.getStyleClass().add("page-subtitle");

        VBox headerBox = new VBox(4, title, subtitle);

        // Controls row
        Label algoLabel = new Label("Algorithm");
        algoLabel.getStyleClass().add("section-title");

        startWord.setPromptText("Starting word (e.g. \"she\", \"the\")");
        HBox.setHgrow(startWord, Priority.ALWAYS);

        maxWords.setEditable(true);
        generateBtn.getStyleClass().add("button-primary");

        HBox controlRow = new HBox(
                8,
                new Label("Algorithm:"), algoPicker,
                new Label("Start:"), startWord,
                new Label("Max words:"), maxWords,
                generateBtn
        );
        controlRow.getStyleClass().add("toolbar-row");

        // Output area
        Label outputLabel = new Label("Generated sentence");
        outputLabel.getStyleClass().add("section-title");

        output.setWrapText(true);
        output.setEditable(false);
        output.setPromptText("Generated sentence will appear here.");

        VBox body = new VBox(
                10,
                headerBox,
                new Separator(),
                algoLabel,
                controlRow,
                new Separator(),
                outputLabel,
                output
        );

        VBox.setVgrow(output, Priority.ALWAYS);

        BorderPane card = new BorderPane(body);
        card.getStyleClass().addAll("page-card", "page-card-wide");
        card.setPadding(new Insets(0));

        setCenter(card);
        BorderPane.setMargin(card, new Insets(0, 0, 16, 0));

        // Algorithm options
        List<SentenceAlgorithm> algorithms = new ArrayList<>();
        algorithms.add(new BigramGreedySentenceAlgorithm());
        algorithms.add(new BigramRandomSentenceAlgorithm());
        algorithms.add(new TrigramGreedySentenceAlgorithm());
        algorithms.add(new AlliterativeSentenceAlgorithm());
        algorithms.add(new SmartTrigramSamplingSentenceAlgorithm());
        // Add additional strategies here if needed.

        algoPicker.getItems().addAll(algorithms);

        algoPicker.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(SentenceAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        algoPicker.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SentenceAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        algoPicker.getSelectionModel().selectFirst();

        // Behavior
        generateBtn.setOnAction(e -> generate());
        startWord.setOnAction(e -> generate()); // Press Enter to generate.
    }

    /**
     * Generate a sentence using the currently selected algorithm and settings.
     * This method performs a database query via the SentenceAlgorithm.
     * For assignment-sized data, it runs synchronously on the FX thread.
     */
    private void generate() {
        SentenceAlgorithm algo = algoPicker.getValue();
        if (algo == null) {
            output.setText("No algorithm selected.");
            return;
        }

        String start = startWord.getText().trim();
        if (start.isEmpty()) {
            output.setText("Please provide a starting word.");
            return;
        }

        try (Connection c = Database.get()) {
            String sentence = algo.generate(c, start, maxWords.getValue());
            output.setText(sentence);
        } catch (Exception ex) {
            output.setText("Error: " + ex.getMessage());
            new Alert(Alert.AlertType.ERROR,
                    "Failed to generate sentence:\n" + ex.getMessage()
            ).showAndWait();
        }
    }
}
