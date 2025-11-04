package org.example.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.example.core.DatasetService;
import org.example.model.FileRecord;

import java.io.File;

public class DatasetsView {
    private final DatasetService datasetService;
    private final TableView<FileRecord> table = new TableView<>();
    private final BorderPane root = new BorderPane();

    public DatasetsView(DatasetService datasetService) {
        this.datasetService = datasetService;

        Button importBtn = new Button("Import Dataset…");
        importBtn.setOnAction(e -> importFile());

        HBox top = new HBox(8, importBtn);
        top.setPadding(new Insets(10));
        root.setTop(top);

        TableColumn<FileRecord, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().filename()));
        nameCol.setPrefWidth(420);

        TableColumn<FileRecord, Number> wordsCol = new TableColumn<>("Word Count");
        wordsCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().wordCount()));
        wordsCol.setPrefWidth(120);

        TableColumn<FileRecord, String> dateCol = new TableColumn<>("Import Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().importDate().toString()));
        dateCol.setPrefWidth(160);

        table.getColumns().setAll(nameCol, wordsCol, dateCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No datasets yet. Click “Import Dataset…” to add a text file."));

        // Right-click remove
        table.setRowFactory(tv -> {
            TableRow<FileRecord> row = new TableRow<>();
            MenuItem delete = new MenuItem("Remove");
            delete.setOnAction(e -> {
                FileRecord fr = row.getItem();
                if (fr != null && confirm("Remove dataset “" + fr.filename() + "”?")) {
                    Task<Void> task = new Task<>() {
                        @Override protected Void call() {
                            datasetService.remove(fr.id());
                            return null;
                        }
                    };
                    task.setOnSucceeded(ev -> refresh());
                    new Thread(task, "remove-dataset").start();
                }
            });
            ContextMenu menu = new ContextMenu(delete);
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );
            return row;
        });

        root.setCenter(table);
        refresh();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void refresh() {
        table.getItems().setAll(datasetService.list());
    }

    private void importFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.text", "*.md"));
        File f = fc.showOpenDialog(root.getScene().getWindow());
        if (f == null) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                datasetService.importFile(f);
                return null;
            }
        };
        task.setOnSucceeded(e -> refresh());
        new Thread(task, "import-dataset").start();
    }

    public Parent getNode() { return root; }
}
