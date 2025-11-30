
package com.sentencebuilder.ui;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.sentencebuilder.dao.Database;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class DatabaseViewerPane extends BorderPane {
    private final ComboBox<String> tablePicker = new ComboBox<>();
    private final Button refreshBtn = new Button("Refresh");
    private final TableView<List<String>> table = new TableView<>();

    public DatabaseViewerPane() {
        getStyleClass().add("content-pane");
        setPadding(new Insets(0));

        Label title = new Label("Database Viewer");
        title.getStyleClass().add("section-title");

        HBox top = new HBox(10, title, new Label("Table:"), tablePicker, refreshBtn);
        top.setPadding(new Insets(0, 0, 10, 0));
        top.getStyleClass().add("card");
        setTop(top);

        table.getStyleClass().add("sub-card");
        setCenter(table);

        tablePicker.getItems().addAll(
            "files",
            "words",
            "word_relations_bigram",
            "word_relations_trigram",
            "word_files",
            "metadata"
        );
        tablePicker.getSelectionModel().selectFirst();

        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());
        refresh();
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
