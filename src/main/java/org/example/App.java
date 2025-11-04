package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.core.*;
import org.example.data.DatasetRepository;
import org.example.ui.RootView;


/**
 * Application entry point.
 * 
 * To test your teammates' algorithm:
 * Replace the NGramModel with their implementation of LanguageModel.
 */

public class App extends Application {
    @Override
    public void start(Stage stage) {
        DatasetRepository repo = new DatasetRepository();
        NGramModel ngram = new NGramModel(repo);
        DatasetService datasetService = new DatasetServiceImpl(repo, ngram);
        SuggestionService suggestionService = ngram;
        SentenceGenerator sentenceGenerator = ngram;

        RootView rootView = new RootView(datasetService, suggestionService, sentenceGenerator, ngram.buildingProperty());
        Scene scene = new Scene(rootView.getNode(), 1000, 700);

        // add default stylesheet (light)
        scene.getStylesheets().add(getClass().getResource("/styles/light.css").toExternalForm());

        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();
    }
}
