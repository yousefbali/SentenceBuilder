/******************************************************************************
 * MainApp
 *
 * Entry point for the Sentence Builder application.
 *
 * Responsibilities:
 *   - Ensure the database is configured and reachable on startup.
 *   - If not, launch the SetupWizard to guide the user through DB setup.
 *   - Construct the main tabbed UI (Import, DB Viewer, Sentence Generator,
 *     Autocomplete, Analytics).
 *   - Wire the Import tab so that, after a successful import, the Analytics
 *     and Database Viewer tabs automatically refresh their views.
 *
 * Written by Yousuf Ismail (yxi220002) for CS 4485.0W1,
 * starting October 17, 2025.
 ******************************************************************************/

package com.sentencebuilder;

import com.sentencebuilder.dao.Database;
import com.sentencebuilder.ui.AnalyticsPane;
import com.sentencebuilder.ui.AutocompletePane;
import com.sentencebuilder.ui.DatabaseViewerPane;
import com.sentencebuilder.ui.ImportDataPane;
import com.sentencebuilder.ui.SentenceGeneratorPane;
import com.sentencebuilder.ui.SetupWizard;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/**
 * JavaFX Application subclass that builds and shows the main window.
 */
public class MainApp extends Application {

    /**
     * JavaFX lifecycle entry point. Called after the toolkit is initialized.
     *
     * @param stage primary Stage provided by JavaFX.
     */
    @Override
    public void start(Stage stage) {
        // Before building the UI, make sure we can talk to the database.
        // If configuration is missing or invalid, we invoke the SetupWizard.
        if (!ensureDatabaseConfigured(stage)) {
            // User cancelled setup or it failed in a way we cannot recover from.
            // In that case we exit the app rather than showing a broken UI.
            return;
        }

        // Main container for the application: each major feature gets its own tab.
        TabPane tabs = new TabPane();

        // Create panes that we need to keep references to (for cross-tab updates).
        DatabaseViewerPane dbViewerPane   = new DatabaseViewerPane();
        AnalyticsPane      analyticsPane  = new AnalyticsPane(stage);
        SentenceGeneratorPane sentenceGeneratorPane = new SentenceGeneratorPane();
        AutocompletePane      autocompletePane      = new AutocompletePane();

        // Import pane will notify analytics + DB viewer when a file is successfully imported.
        ImportDataPane importPane = new ImportDataPane(() -> {
            // Refresh DB viewer so it shows the latest rows after imports.
            dbViewerPane.refreshData();

            // Refresh analytics so summary stats and top words stay up to date.
            analyticsPane.refreshData();
        });

        // Build tabs and add them to the TabPane.
        tabs.getTabs().add(new Tab("Import Data",        importPane));
        tabs.getTabs().add(new Tab("Database Viewer",    dbViewerPane));
        tabs.getTabs().add(new Tab("Sentence Generator", sentenceGeneratorPane));
        tabs.getTabs().add(new Tab("Autocomplete",       autocompletePane));
        tabs.getTabs().add(new Tab("Analytics",          analyticsPane));

        // We do not want the user to accidentally close tabs; keep them fixed.
        for (Tab t : tabs.getTabs()) {
            t.setClosable(false);
        }

        // Build the Scene with a reasonable default size and attach our stylesheet.
        Scene scene = new Scene(tabs, 1100, 700);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );

        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Ensure that the database configuration is valid and reachable.
     *
     * Behavior:
     *   1. Try a quick testConnection() using whatever config is currently available.
     *   2. If that fails, show the SetupWizard to let the user enter MySQL admin
     *      info and create the sentencegen DB + user.
     *   3. After the wizard closes, try testConnection() again.
     *
     * @param owner Stage used as the owner for dialogs (alerts, wizard).
     * @return true if a DB connection is possible, false if the app should exit.
     */
    private boolean ensureDatabaseConfigured(Stage owner) {
        try {
            // Attempt to connect using existing configuration (if any).
            Database.testConnection();
            return true;
        } catch (Exception e) {
            // Initial connection failed. This usually means:
            //   - No db.properties yet, or
            //   - Wrong credentials/host, or
            //   - MySQL not running.
            // In any of those cases, we give the user a chance to fix it via the wizard.
            SetupWizard wizard = new SetupWizard(owner);
            wizard.showAndWait();

            if (!wizard.isSuccess()) {
                // The wizard either failed or the user clicked Cancel.
                // Show a clear message and exit gracefully.
                Alert alert = new Alert(
                        Alert.AlertType.ERROR,
                        "Database setup was not completed. The application will exit."
                );
                alert.showAndWait();
                return false;
            }

            // If setup reported success, we immediately try to connect again.
            try {
                Database.testConnection();
                return true;
            } catch (Exception ex) {
                // Even after setup we could not connect; show the underlying error.
                Alert alert = new Alert(
                        Alert.AlertType.ERROR,
                        "Could not connect to the database even after setup: " + ex.getMessage()
                );
                alert.showAndWait();
                return false;
            }
        }
    }

    /**
     * Traditional main method used when launching as a standalone Java app.
     *
     * @param args command-line arguments (unused).
     */
    public static void main(String[] args) {
        launch(args);
    }
}

