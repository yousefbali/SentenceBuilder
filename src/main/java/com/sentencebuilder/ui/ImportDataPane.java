/******************************************************************************
 * ImportDataPane
 *
 * UI pane that allows the user to:
 *   - Choose a plain text file from disk.
 *   - Tokenize the file using a TextTokenizer implementation.
 *   - Stream the token statistics into the database via ImportService.
 *   - Monitor progress and see a summary of the import inline.
 *
 * The import is run on a background Task so that the UI remains responsive.
 * An optional callback can be supplied to notify other panes (analytics,
 * database viewer, etc.) when new data has been imported.
 *
 * Written by Yousef Ali  for CS 4485.0W1,
 * starting October 23, 2025.
 ******************************************************************************/

package com.sentencebuilder.ui;

import com.sentencebuilder.services.ImportService;
import com.sentencebuilder.tokenize.SimpleWhitespaceTokenizer;
import com.sentencebuilder.tokenize.TextTokenizer;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;

/**
 * JavaFX pane for importing corpus text into the Sentence Builder database.
 */
public class ImportDataPane extends BorderPane {

    /** Text field that displays the selected file path (read-only). */
    private final TextField fileField = new TextField();

    /** Button that opens a file chooser to pick a text file. */
    private final Button chooseBtn = new Button("Choose file…");

    /** Button that starts the import operation. */
    private final Button submitBtn = new Button("Submit import");

    /** Button that cancels the currently running import (if any). */
    private final Button cancelBtn = new Button("Cancel");

    /** Progress bar for the background import task. */
    private final ProgressBar progress = new ProgressBar(0);

    /** Label above the progress bar describing what is being imported. */
    private final Label progressLabel = new Label("Progress");

    /** Status label showing current state (Idle, Importing, Failed, etc.). */
    private final Label status = new Label("Idle");

    /** Label heading for the inline import summary. */
    private final Label summaryTitle = new Label("Last import summary");

    /** Simple multi-line label where we print the last import summary. */
    private final Label summaryBody = new Label("No imports yet.");

    /** File selected for import, or null if none chosen yet. */
    private File selected;

    /** Current background import task, used for cancellation/status. */
    private Task<ImportService.ImportSummary> currentTask;

    /**
     * Optional callback invoked when an import finishes successfully.
     * The main window can use this to refresh analytics or other dependent panes.
     */
    private final Runnable onImportFinished;

    /**
     * Default constructor (no completion callback).
     */
    public ImportDataPane() {
        this(null);
    }

    /**
     * Construct an ImportDataPane with an optional completion callback.
     *
     * @param onImportFinished runnable to run after a successful import, or
     *                         {@code null} if no callback is needed.
     */
    public ImportDataPane(Runnable onImportFinished) {
        this.onImportFinished = onImportFinished;

        // Page styling
        getStyleClass().add("page-root");

        // Header
        Label title = new Label("Import text data");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label(
                "Choose a plain text file, tokenize it, and load its word statistics into the database."
        );
        subtitle.getStyleClass().add("page-subtitle");

        VBox headerBox = new VBox(4, title, subtitle);

        // File selector row
        Label fileSectionLabel = new Label("Text file");
        fileSectionLabel.getStyleClass().add("section-title");

        fileField.setEditable(false);
        fileField.setPromptText("No file selected");
        fileField.setPrefColumnCount(40);

        chooseBtn.getStyleClass().add("button-primary");
        submitBtn.getStyleClass().add("button-primary");
        cancelBtn.getStyleClass().add("button-secondary");

        HBox fileRow = new HBox(8, fileField, chooseBtn);
        fileRow.getStyleClass().add("toolbar-row");
        HBox.setHgrow(fileField, Priority.ALWAYS);

        // Action buttons row
        HBox actionsRow = new HBox(8, submitBtn, cancelBtn);
        actionsRow.getStyleClass().add("toolbar-row");
        cancelBtn.setDisable(true); // nothing to cancel initially

        // Progress + status
        progress.setPrefWidth(260);

        Label progressSectionLabel = new Label("Import progress");
        progressSectionLabel.getStyleClass().add("section-title");

        status.getStyleClass().add("status-label");

        // Summary label setup: multi-line, no scrollbars, wraps nicely in the white box.
        summaryTitle.getStyleClass().add("section-title");
        summaryBody.setWrapText(true);
        summaryBody.getStyleClass().add("summary-body");

        VBox progressBox = new VBox(
                6,
                progressSectionLabel,
                progressLabel,
                progress,
                status,
                new Separator(),
                summaryTitle,
                summaryBody
        );

        // Inner card layout
        VBox card = new VBox(
                16,
                headerBox,
                fileSectionLabel,
                fileRow,
                actionsRow,
                new Separator(),
                progressBox
        );
        card.getStyleClass().addAll("page-card", "page-card-wide");
        card.setPadding(new Insets(0));

        setCenter(card);
        BorderPane.setMargin(card, new Insets(0, 0, 16, 0));

        // Behavior

        // Let the user choose a file from disk.
        chooseBtn.setOnAction(e -> chooseFile());

        // Start an import when the user clicks "Submit import".
        submitBtn.setOnAction(e -> startImport());

        // Allow cancellation of a running import.
        cancelBtn.setOnAction(e -> {
            if (currentTask != null && currentTask.isRunning()) {
                currentTask.cancel();
                status.setText("Cancelling...");
            }
        });
    }

    /**
     * Open a FileChooser so the user can pick a text file to import.
     *
     * Default directory:
     *   - If a local "input" directory exists under the current working dir,
     *     we start there (SentenceBuilder/input).
     *   - Otherwise, we let the OS default decide.
     */
    private void chooseFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose text file to import");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.text"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Try to default to ./input (your SentenceBuilder/input folder)
        File cwd = new File(System.getProperty("user.dir"));
        File inputDir = new File(cwd, "input");
        if (inputDir.isDirectory()) {
            fc.setInitialDirectory(inputDir);
        }

        File chosen = fc.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (chosen != null) {
            selected = chosen;
            fileField.setText(chosen.getAbsolutePath());
            status.setText("Selected file.");
            progressLabel.setText("Progress: " + chosen.getName());
        }
    }

    /**
     * Start a new background import Task using the currently selected file.
     * Validates that a file has been chosen and that there is no running import.
     */
    private void startImport() {
        if (selected == null) {
            status.setText("Please choose a file first.");
            return;
        }
        if (currentTask != null && currentTask.isRunning()) {
            status.setText("An import is already running.");
            return;
        }

        TextTokenizer tokenizer = new SimpleWhitespaceTokenizer();
        ImportService svc = new ImportService();

        // Disable controls while the import is in progress so the user
        // can't start multiple imports or change the file mid-run.
        submitBtn.setDisable(true);
        chooseBtn.setDisable(true);
        cancelBtn.setDisable(false);

        progress.setProgress(0);
        progressLabel.setText("Progress: " + selected.getName());

        Task<ImportService.ImportSummary> task = new Task<>() {
            @Override
            protected ImportService.ImportSummary call() throws Exception {
                // This message is bound to the status label.
                updateMessage("Importing " + selected.getName() + "...");
                // Indeterminate progress while ImportService does its work.
                updateProgress(-1, 1.0);

                ImportService.ImportSummary summary = svc.importFile(selected, tokenizer);

                if (isCancelled()) {
                    // If we were cancelled mid-import, don't treat this as a success.
                    updateMessage("Cancelled.");
                    return null;
                }

                // Mark as done and flip the bar to 100%.
                updateMessage("Finalizing import...");
                updateProgress(1.0, 1.0);
                updateMessage("Done.");
                return summary;
            }
        };

        // On success, display summary and reset controls.
        task.setOnSucceeded(ev -> {
            // Unbind status/progress now that the task is finished.
            progress.progressProperty().unbind();
            status.textProperty().unbind();

            ImportService.ImportSummary summary = task.getValue();
            if (summary != null) {
                // Format a neat multi-line summary for the label.
                String msg = String.format(
                        "File: %s%n" +
                        "Tokens: %d%n" +
                        "Distinct words: %d%n" +
                        "Bigrams: %d%n" +
                        "Trigrams: %d",
                        summary.filename(),
                        summary.totalTokens(),
                        summary.distinctWords(),
                        summary.bigramCount(),
                        summary.trigramCount()
                );

                status.setText("Done.");
                progressLabel.setText("Completed: " + summary.filename());
                progress.setProgress(1.0);
                summaryBody.setText(msg);
            } else {
                // This is the case where the Task says "success" but returned null
                // (for example if it was cancelled very late).
                status.setText("Done.");
                progressLabel.setText("Completed (no summary).");
                progress.setProgress(1.0);
                summaryBody.setText("Completed import (no summary available).");
            }

            // Re-enable buttons for the next import.
            submitBtn.setDisable(false);
            chooseBtn.setDisable(false);
            cancelBtn.setDisable(true);

            // Notify other panes that data has changed (analytics, DB viewer, etc.).
            if (onImportFinished != null) {
                onImportFinished.run();
            }

            // Reset progress bar + status after a short delay if nothing else has started.
            PauseTransition resetDelay = new PauseTransition(Duration.seconds(5));
            resetDelay.setOnFinished(e2 -> {
                if (currentTask == null || !currentTask.isRunning()) {
                    progress.setProgress(0);
                    status.setText("Idle");
                    progressLabel.setText("Progress");
                }
            });
            resetDelay.play();
        });

        // On failure, show an error and reset controls.
        task.setOnFailed(ev -> {
            progress.progressProperty().unbind();
            status.textProperty().unbind();

            // Capture the root cause so we can print something useful in the UI and alert.
            Throwable t = task.getException();
            String msg = (t == null)
                    ? "Unknown error."
                    : (t.getClass().getSimpleName() + ": " + t.getMessage());

            status.setText("Failed: " + msg);
            submitBtn.setDisable(false);
            chooseBtn.setDisable(false);
            cancelBtn.setDisable(true);

            // Show the same message inline under "Last import summary".
            summaryBody.setText("Import failed:\n" + msg);

            // And also in a popup dialog so the user definitely sees it.
            new Alert(Alert.AlertType.ERROR, "Import failed:\n" + msg).showAndWait();

            // After a short delay, reset progress + status back to idle
            // (as long as nothing else has started).
            PauseTransition resetDelay = new PauseTransition(Duration.seconds(5));
            resetDelay.setOnFinished(e2 -> {
                if (currentTask == null || !currentTask.isRunning()) {
                    progress.setProgress(0);
                    status.setText("Idle");
                    progressLabel.setText("Progress");
                }
            });
            resetDelay.play();
        });

        // On cancellation, show a simple cancelled message and reset controls.
        task.setOnCancelled(ev -> {
            progress.progressProperty().unbind();
            status.textProperty().unbind();

            status.setText("Cancelled.");
            summaryBody.setText("Import cancelled.");
            submitBtn.setDisable(false);
            chooseBtn.setDisable(false);
            cancelBtn.setDisable(true);

            PauseTransition resetDelay = new PauseTransition(Duration.seconds(5));
            resetDelay.setOnFinished(e2 -> {
                if (currentTask == null || !currentTask.isRunning()) {
                    progress.setProgress(0);
                    status.setText("Idle");
                    progressLabel.setText("Progress");
                }
            });
            resetDelay.play();
        });

        // Bind the Task's progress/message to the UI controls while it runs.
        progress.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());

        currentTask = task;

        Thread worker = new Thread(task, "import-thread");
        worker.setDaemon(true);
        worker.start();
    }
}

