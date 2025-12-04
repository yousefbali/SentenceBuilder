/******************************************************************************
 * AnalyticsPane
 *
 * UI pane that provides a quick overview of the imported corpus:
 *   - Number of imported files.
 *   - Vocabulary size (distinct words).
 *   - Total token count (sum of word frequencies).
 *   - Number of bigram and trigram relations.
 *   - A sortable table of the most frequent words.
 *
 * It depends on the database schema used by Sentence Builder and reloads its
 * data via the DataRefreshable interface after imports or resets.
 *
 * Written by Yousef Ali (yba210001) for CS 4485.0W1,
 * starting October 23, 2025.
 ******************************************************************************/

package com.sentencebuilder.ui;

import com.sentencebuilder.dao.Database;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JavaFX pane that shows summary analytics and a top-words table.
 */
public class AnalyticsPane extends BorderPane implements DataRefreshable {

    /** Owning stage (used for dialogs launched from this pane, if needed). */
    private final Stage owner;

    /** Summary labels for counts. */
    private final Label totalFilesLabel   = new Label("-");
    private final Label totalWordsLabel   = new Label("-");
    private final Label totalTokensLabel  = new Label("-");
    private final Label bigramCountLabel  = new Label("-");
    private final Label trigramCountLabel = new Label("-");

    /** Table listing the top words by frequency. */
    private final TableView<WordStat> topWordsTable = new TableView<>();

    /**
     * Construct the analytics pane for a given owner window.
     *
     * @param owner primary stage or parent dialog that owns this pane.
     */
    public AnalyticsPane(Stage owner) {
        this.owner = owner;

        // Page background
        getStyleClass().add("page-root");

        // Header
        Label title = new Label("Corpus analytics");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label(
                "Quick snapshot of your imported text: token counts, n-gram coverage, and word frequencies."
        );
        subtitle.getStyleClass().add("page-subtitle");

        VBox headerBox = new VBox(4, title, subtitle);

        // Summary stats
        Label summaryLabel = new Label("Summary");
        summaryLabel.getStyleClass().add("section-title");

        GridPane summary = new GridPane();
        summary.setHgap(32);
        summary.setVgap(6);

        int row = 0;
        summary.add(new Label("Imported files:"), 0, row);
        summary.add(totalFilesLabel, 1, row++);

        summary.add(new Label("Vocabulary size (distinct words):"), 0, row);
        summary.add(totalWordsLabel, 1, row++);

        summary.add(new Label("Total tokens (after tokenization):"), 0, row);
        summary.add(totalTokensLabel, 1, row++);

        summary.add(new Label("Bigram relations:"), 0, row);
        summary.add(bigramCountLabel, 1, row++);

        summary.add(new Label("Trigram relations:"), 0, row);
        summary.add(trigramCountLabel, 1, row++);

        // Top words table
        Label topLabel = new Label("Top words");
        topLabel.getStyleClass().add("section-title");

        TableColumn<WordStat, String> wordCol = new TableColumn<>("Word");
        wordCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().word()));

        TableColumn<WordStat, String> totalCol = new TableColumn<>("Total count");
        totalCol.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().totalCount()))
        );

        TableColumn<WordStat, String> startCol = new TableColumn<>("Sentence starts");
        startCol.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().startCount()))
        );

        TableColumn<WordStat, String> endCol = new TableColumn<>("Sentence ends");
        endCol.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().endCount()))
        );

        topWordsTable.getColumns().addAll(wordCol, totalCol, startCol, endCol);
        topWordsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        VBox.setVgrow(topWordsTable, Priority.ALWAYS);

        // Reset application button (opens ResetWizard)
        Button resetButton = new Button("Reset Application...");
        resetButton.getStyleClass().add("button-danger");
        resetButton.setOnAction(e -> {
            ResetWizard wizard = new ResetWizard(owner);
            wizard.showAndWait();
        });

        HBox resetRow = new HBox(resetButton);
        resetRow.setAlignment(Pos.CENTER_RIGHT);

        // Card layout
        VBox card = new VBox(
                16,
                headerBox,
                summaryLabel,
                summary,
                new Separator(),
                topLabel,
                topWordsTable,
                new Separator(),
                resetRow
        );
        card.getStyleClass().addAll("page-card", "page-card-wide");
        card.setPadding(new Insets(0));

        setCenter(card);
        BorderPane.setMargin(card, new Insets(0, 0, 16, 0));

        // Initial load
        refresh();
    }

    /**
     * Refresh both the summary stats and the top-words table from the database.
     */
    private void refresh() {
        try (Connection c = Database.get()) {
            loadSummaryStats(c);
            loadTopWords(c);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load analytics: " + ex.getMessage()
            ).showAndWait();
        }
    }

    /**
     * Load and display summary counts from the database.
     *
     * @param c open JDBC connection.
     */
    private void loadSummaryStats(Connection c) throws SQLException {
        totalFilesLabel.setText(String.valueOf(
                singleInt(c, "SELECT COUNT(*) FROM files")
        ));

        totalWordsLabel.setText(String.valueOf(
                singleInt(c, "SELECT COUNT(*) FROM words")
        ));

        totalTokensLabel.setText(String.valueOf(
                singleInt(c, "SELECT COALESCE(SUM(total_count), 0) FROM words")
        ));

        bigramCountLabel.setText(String.valueOf(
                singleInt(c, "SELECT COUNT(*) FROM word_relations_bigram")
        ));

        trigramCountLabel.setText(String.valueOf(
                singleInt(c, "SELECT COUNT(*) FROM word_relations_trigram")
        ));
    }

    /**
     * Load the top words by total count and show them in the table.
     * Sorting is handled by TableView, so we load all rows ordered by total_count.
     *
     * @param c open JDBC connection.
     */
    private void loadTopWords(Connection c) throws SQLException {
        ObservableList<WordStat> items = FXCollections.observableArrayList();

        String sql = "SELECT word_text, total_count, sentence_start_count, sentence_end_count " +
                     "FROM words ORDER BY total_count DESC";

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String word = rs.getString("word_text");
                int total = rs.getInt("total_count");
                int starts = rs.getInt("sentence_start_count");
                int ends = rs.getInt("sentence_end_count");

                items.add(new WordStat(word, total, starts, ends));
            }
        }

        topWordsTable.setItems(items);
    }

    /**
     * Convenience helper for queries that return a single integer.
     *
     * @param c   open JDBC connection.
     * @param sql SQL query that returns a single integer in column 1.
     * @return that integer, or 0 if the result set is empty.
     */
    private int singleInt(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * Called from outside when the underlying DB has changed (e.g., after an
     * import). Resets state and reloads analytics.
     */
    @Override
    public void refreshData() {
        refresh();
    }

    /**
     * Simple immutable holder for per-word statistics displayed in the table.
     */
    private static class WordStat {
        private final String word;
        private final int totalCount;
        private final int startCount;
        private final int endCount;

        WordStat(String word, int totalCount, int startCount, int endCount) {
            this.word = word;
            this.totalCount = totalCount;
            this.startCount = startCount;
            this.endCount = endCount;
        }

        public String word()    { return word; }
        public int totalCount() { return totalCount; }
        public int startCount() { return startCount; }
        public int endCount()   { return endCount; }
    }
}

