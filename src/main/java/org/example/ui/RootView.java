package org.example.ui;

import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.ProgressIndicator;
import org.example.core.DatasetService;
import org.example.core.SentenceGenerator;
import org.example.core.SuggestionService;

public class RootView {
    private final BorderPane root = new BorderPane();

    public RootView(DatasetService datasetService,
                    SuggestionService suggestionService,
                    SentenceGenerator sentenceGenerator,
                    ObservableBooleanValue modelBuilding) {

        // Menu bar with View -> Dark Mode toggle
        MenuBar menuBar = new MenuBar();
        Menu view = new Menu("View");
        CheckMenuItem darkMode = new CheckMenuItem("Dark mode");
        view.getItems().add(darkMode);
        menuBar.getMenus().add(view);

        darkMode.selectedProperty().addListener((obs, was, is) -> {
            if (root.getScene() == null) return;
            var styles = root.getScene().getStylesheets();
            styles.clear();
            String css = is ? "/styles/dark.css" : "/styles/light.css";
            styles.add(getClass().getResource(css).toExternalForm());
        });

        // Tabs
        TabPane tabs = new TabPane();
        Tab compose = new Tab("Compose");
        compose.setClosable(false);
        compose.setContent(new ComposeView(suggestionService, sentenceGenerator).getNode());

        Tab datasets = new Tab("Datasets");
        datasets.setClosable(false);
        datasets.setContent(new DatasetsView(datasetService).getNode());

        tabs.getTabs().addAll(compose, datasets);

        // Status bar (bottom): shows build progress
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(16, 16);
        spinner.visibleProperty().bind(modelBuilding);

        Label status = new Label();
        status.textProperty().bind(
                modelBuilding.map(b -> b ? "Building language model…" : "Ready")
        );

        HBox statusBar = new HBox(8, spinner, status);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(6,10,6,10));

        root.setTop(menuBar);
        root.setCenter(tabs);
        root.setBottom(statusBar);
    }

    public Parent getNode() { return root; }
}
