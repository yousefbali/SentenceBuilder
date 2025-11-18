package com.sentencebuilder.ui;

import com.sentencebuilder.dao.Database;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AnalyticsPane extends BorderPane {

    private final Label totalFilesLabel = new Label("-");
    private final Label totalWordsLabel = new Label("-");
    private final Label totalTokensLabel = new Label("-");
    private final Label bigramCountLabel = new Label("-");
    private final Label trigramCountLabel = new Label("-");

    private final TableView<WordStat> topWordsTable = new TableView<>();
    private final Button refreshBtn = new Button("Refresh");

    public AnalyticsPane() {
        setPadding(new Insets(15));

        // -------- Summary stats grid --------
        GridPane summary = new GridPane();
        summary.setHgap(20);
        summary.setVgap(5);

        int row = 0;
        summary.add(new Label("Imported files:"), 0, row);
        summary.add(totalFilesLabel, 1, row++);

        summary.add(new Label("Vocabulary size (distinct words):"), 0, row);
        summary.add(totalWordsLabel, 1, row++);

        summary.add(new Label("Total tokens (all files):"), 0, row);
        summary.add(totalTokensLabel, 1, row++);

        row = 0;
        summary.add(new Label("Distinct bigrams:"), 2, row);
        summary.add(bigramCountLabel, 3, row++);

        summary.add(new Label("Distinct trigrams:"), 2, row);
        summary.add(trigramCountLabel, 3, row);

        HBox topBar = new HBox(20, summary, refreshBtn);
        topBar.setPadding(new Insets(0, 0, 10, 0));
        setTop(topBar);

        // -------- Top words table --------
        TableColumn<WordStat, String> wordCol = new TableColumn<>("Word");
        wordCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().word()));
        wordCol.setPrefWidth(200);

        TableColumn<WordStat, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().totalCount())
        ));
        totalCol.setPrefWidth(80);

        TableColumn<WordStat, String> startCol = new TableColumn<>("Starts");
        startCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().startCount())
        ));
        startCol.setPrefWidth(80);

        TableColumn<WordStat, String> endCol = new TableColumn<>("Ends");
        endCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().endCount())
        ));
        endCol.setPrefWidth(80);

        topWordsTable.getColumns().clear();
        topWordsTable.getColumns().add(wordCol);
        topWordsTable.getColumns().add(totalCol);
        topWordsTable.getColumns().add(startCol);
        topWordsTable.getColumns().add(endCol);


        topWordsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        VBox centerBox = new VBox(10,
                new Label("Top words by total frequency"),
                topWordsTable
        );
        centerBox.setPadding(new Insets(10, 0, 0, 0));
        setCenter(centerBox);

        // -------- Wiring --------
        refreshBtn.setOnAction(e -> refresh());
        refresh();
    }

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

    private void loadSummaryStats(Connection c) throws SQLException {
        totalFilesLabel.setText(String.valueOf(
                singleInt(c, "SELECT COUNT(*) FROM files")
        ));
        totalWordsLabel.setText(String.valueOf(
                singleInt(c, "SELECT COUNT(*) FROM words")
        ));
        totalTokensLabel.setText(String.valueOf(
                singleInt(c, "SELECT COALESCE(SUM(word_count), 0) FROM files")
        ));
        bigramCountLabel.setText(String.valueOf(
                singleInt(c, "SELECT COUNT(*) FROM word_relations_bigram")
        ));
        trigramCountLabel.setText(String.valueOf(
                singleInt(c, "SELECT COUNT(*) FROM word_relations_trigram")
        ));
    }

private void loadTopWords(Connection c) throws SQLException {
    ObservableList<WordStat> items = FXCollections.observableArrayList();

    String sql = """
        SELECT word_text, total_count, sentence_start_count, sentence_end_count
        FROM words
        ORDER BY total_count DESC
        LIMIT 50
        """;

    try (PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            items.add(new WordStat(
                    rs.getString("word_text"),
                    rs.getInt("total_count"),
                    rs.getInt("sentence_start_count"),
                    rs.getInt("sentence_end_count")
            ));
        }
    }

    topWordsTable.setItems(items);
}


    private int singleInt(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    // simple holder for a row in the table
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

        public String word()        { return word; }
        public int totalCount()     { return totalCount; }
        public int startCount()     { return startCount; }
        public int endCount()       { return endCount; }
    }
}
