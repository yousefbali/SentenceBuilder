package com.sentencebuilder.ui;

import com.sentencebuilder.services.ImportService;
import com.sentencebuilder.tokenize.SimpleWhitespaceTokenizer;
import com.sentencebuilder.tokenize.TextTokenizer;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class ImportDataPane extends BorderPane {
    private final TextField fileField = new TextField();
    private final Button chooseBtn = new Button("Choose File...");
    private final Button submitBtn = new Button("Submit Import");
    private final Button cancelBtn = new Button("Cancel");
    private final ProgressBar progress = new ProgressBar(0);
    private final Label status = new Label("Idle");
    private File selected;
    private Thread worker;

    public ImportDataPane() {
        setPadding(new Insets(15));
        HBox top = new HBox(10, new Label("Text file:"), fileField, chooseBtn, submitBtn, cancelBtn);
        fileField.setPrefColumnCount(50);
        fileField.setEditable(false);
        setTop(top);
        VBox center = new VBox(10, new Separator(), new Label("Progress:"), progress, status);
        center.setPadding(new Insets(10,0,0,0));
        setCenter(center);

        chooseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files","*.txt","*.text"),
                new FileChooser.ExtensionFilter("All Files","*.*")
            );
            File f = fc.showOpenDialog(getScene() == null ? null : getScene().getWindow());
            if (f != null) {
                selected = f;
                fileField.setText(f.getAbsolutePath());
                status.setText("Selected file.");
            }
        });

        submitBtn.setOnAction(e -> startImport());
        cancelBtn.setOnAction(e -> {
            if (worker != null && worker.isAlive()) {
                worker.interrupt();
                status.setText("Cancelling...");
            }
        });
    }

    private void startImport() {
        if (selected == null) {
            status.setText("Please choose a file first.");
            return;
        }
        TextTokenizer tokenizer = new SimpleWhitespaceTokenizer(); // swap with your team's tokenizer later
        ImportService svc = new ImportService();
        submitBtn.setDisable(true);
        chooseBtn.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                updateMessage("Importing " + selected.getName() + "...");
                updateProgress(-1, 1);
                try {
                    svc.importFile(selected, tokenizer);
                    if (isCancelled()) return null;
                    updateProgress(1, 1);
                    updateMessage("Done.");
                } catch (Throwable t) {
                    updateMessage("Error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    throw t;
                }
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            progress.progressProperty().unbind();
            status.textProperty().unbind();
            progress.setProgress(1);
            status.setText("Done.");
            submitBtn.setDisable(false);
            chooseBtn.setDisable(false);
        });
        task.setOnFailed(ev -> {
            progress.progressProperty().unbind();
            status.textProperty().unbind();
            Throwable t = task.getException();
            String msg = (t == null) ? "Unknown error." : (t.getClass().getSimpleName() + ": " + t.getMessage());
            status.setText("Failed: " + msg);
            submitBtn.setDisable(false);
            chooseBtn.setDisable(false);
            new Alert(Alert.AlertType.ERROR, "Import failed:\n" + msg).showAndWait();
        });
        task.setOnCancelled(ev -> {
            progress.progressProperty().unbind();
            status.textProperty().unbind();
            status.setText("Cancelled.");
            submitBtn.setDisable(false);
            chooseBtn.setDisable(false);
        });

        progress.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());
        worker = new Thread(task, "import-thread");
        worker.setDaemon(true);
        worker.start();
    }
}
