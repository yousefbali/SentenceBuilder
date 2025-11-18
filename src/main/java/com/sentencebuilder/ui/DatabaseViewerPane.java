
package com.sentencebuilder.ui;

import com.sentencebuilder.dao.Database;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseViewerPane extends BorderPane {
    private final ComboBox<String> tablePicker = new ComboBox<>();
    private final Button refreshBtn = new Button("Refresh");
    private final TableView<List<String>> table = new TableView<>();

    public DatabaseViewerPane() {
        setPadding(new Insets(15));
        HBox top = new HBox(10, new Label("Table:"), tablePicker, refreshBtn);
        setTop(top);
        setCenter(table);

        tablePicker.getItems().addAll("files", "words", "word_relations_bigram", "word_relations_trigram", "word_files", "metadata");
        tablePicker.getSelectionModel().selectFirst();

        refreshBtn.setOnAction(e -> refresh());
        refresh(); // initial load
    }

    private void refresh() {
        String tableName = tablePicker.getValue();
        if (tableName == null) return;

        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tableName + " LIMIT 500")) {

            table.getItems().clear();
            table.getColumns().clear();

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                final int colIndex = i-1;
                TableColumn<List<String>, String> col = new TableColumn<>(md.getColumnLabel(i));
                col.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get(colIndex)));
                table.getColumns().add(col);
            }

            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= cols; i++) {
                    row.add(String.valueOf(rs.getObject(i)));
                }
                table.getItems().add(row);
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to load table: " + ex.getMessage()).showAndWait();
        }
    }
}
