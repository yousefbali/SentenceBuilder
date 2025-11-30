package com.sentencebuilder;

import com.sentencebuilder.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("main-tabs");

        tabs.getTabs().add(new Tab("Import Data", new ImportDataPane()));
        tabs.getTabs().add(new Tab("Database Viewer", new DatabaseViewerPane()));
        tabs.getTabs().add(new Tab("Sentence Generator", new SentenceGeneratorPane()));
        tabs.getTabs().add(new Tab("Autocomplete", new AutocompletePane()));
        tabs.getTabs().add(new Tab("Analytics", new AnalyticsPane()));

        for (Tab t : tabs.getTabs()) {
            t.setClosable(false);
        }

        BorderPane root = new BorderPane(tabs);
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );

        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
