
package com.sentencebuilder;

import com.sentencebuilder.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        TabPane tabs = new TabPane();

        tabs.getTabs().add(new Tab("Import Data", new ImportDataPane()));
        tabs.getTabs().add(new Tab("Database Viewer", new DatabaseViewerPane()));
        tabs.getTabs().add(new Tab("Sentence Generator", new SentenceGeneratorPane()));
        tabs.getTabs().add(new Tab("Autocomplete", new AutocompletePane()));
        tabs.getTabs().add(new Tab("Analytics", new AnalyticsPane()));

        for (Tab t : tabs.getTabs()) t.setClosable(false);

        Scene scene = new Scene(tabs, 1100, 700);
        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
