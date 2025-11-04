package org.example.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.core.SentenceGenerator;
import org.example.core.SuggestionService;

import java.util.List;

public class ComposeView {
    private final SuggestionService suggestions;
    private final SentenceGenerator generator;

    private final BorderPane root = new BorderPane();
    private final TextArea input = new TextArea();
    private final FlowPane suggestionBar = new FlowPane(8, 8);
    private final PauseTransition debounce = new PauseTransition(Duration.millis(250));

    public ComposeView(SuggestionService suggestions, SentenceGenerator generator) {
        this.suggestions = suggestions;
        this.generator = generator;

        Button genBtn = new Button("Generate (Ctrl+Enter)");
        genBtn.setOnAction(e -> doGenerate());

        HBox toolbar = new HBox(8, genBtn);
        toolbar.setPadding(new Insets(10));
        root.setTop(toolbar);

        input.setPromptText("Type here…");
        input.setWrapText(true);
        input.setPrefRowCount(12);

        suggestionBar.setPadding(new Insets(4,0,0,0));

        VBox center = new VBox(8, input, new Label("Suggestions:"), suggestionBar);
        center.setPadding(new Insets(10));
        root.setCenter(center);

        input.textProperty().addListener((obs, oldV, newV) -> {
            debounce.stop();
            debounce.setOnFinished(ev -> updateSuggestions(newV));
            debounce.playFromStart();
        });

        // Shortcut: Ctrl+Enter to generate
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.ENTER) {
                doGenerate();
                e.consume();
            }
        });
    }

    private void doGenerate() {
        String out = generator.generate(input.getText(), 12);
        input.setText(out);
        input.positionCaret(out.length());
        updateSuggestions(out);
    }

    private void updateSuggestions(String text) {
        List<String> list = suggestions.suggestNext(text, 5);
        suggestionBar.getChildren().clear();
        for (String s : list) {
            Button chip = new Button(s);
            chip.getStyleClass().add("suggestion-chip");
            chip.setOnAction(e -> appendWord(s));
            suggestionBar.getChildren().add(chip);
        }
    }

    private void appendWord(String w) {
        String t = input.getText();
        if (t == null || t.isBlank()) input.setText(w);
        else input.setText(t + (t.endsWith(" ") ? "" : " ") + w);
        input.positionCaret(input.getText().length());
        updateSuggestions(input.getText());
    }

    public Parent getNode() { return root; }
}
