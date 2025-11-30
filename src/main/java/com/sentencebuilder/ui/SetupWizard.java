/******************************************************************************
 * SetupWizard
 *
 * Modal dialog used on first launch (or when configuration is missing) to:
 *   - Collect MySQL host, port, admin username, and admin password.
 *   - Call DatabaseBootstrap.runSetup to:
 *       * Create the sentencegen database and run schema.sql.
 *       * Create a restricted application user.
 *       * Persist db.properties for future runs.
 *
 * The wizard reports success/failure through the isSuccess() flag so that
 * callers can decide whether to continue launching the main UI.
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.ui;

import com.sentencebuilder.dao.DatabaseBootstrap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Simple wizard-style stage to guide initial DB setup.
 */
public class SetupWizard extends Stage {

    /** True if setup completed successfully. */
    private boolean success = false;

    /**
     * Construct a new SetupWizard.
     *
     * @param owner owner stage so this dialog can be modal relative to it.
     */
    public SetupWizard(Stage owner) {
        setTitle("Database Setup - Sentence Builder");
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);

        // Fields
        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("3306");
        TextField userField = new TextField("root");
        PasswordField passField = new PasswordField();

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));

        int row = 0;

        grid.add(new Label("MySQL host:"), 0, row);
        grid.add(hostField, 1, row++);

        grid.add(new Label("MySQL port:"), 0, row);
        grid.add(portField, 1, row++);

        grid.add(new Label("Admin username:"), 0, row);
        grid.add(userField, 1, row++);

        grid.add(new Label("Admin password:"), 0, row);
        grid.add(passField, 1, row++);

        Label infoLabel = new Label(
                "Enter MySQL admin credentials. The wizard will create the\n" +
                "sentencegen database, initialize tables, and save app settings."
        );
        grid.add(infoLabel, 0, row, 2, 1);

        // Buttons
        Button cancelBtn = new Button("Cancel");
        Button runBtn = new Button("Run Setup");

        HBox buttons = new HBox(8, cancelBtn, runBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        grid.add(buttons, 0, ++row, 2, 1);

        // Behavior
        cancelBtn.setOnAction(e -> {
            success = false;
            close();
        });

        runBtn.setOnAction(e -> {
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();
            String adminUser = userField.getText().trim();
            String adminPass = passField.getText();

            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException ex) {
                showError("Port must be a number.");
                return;
            }

            try {
                DatabaseBootstrap.runSetup(host, port, adminUser, adminPass);
                success = true;
                Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Database setup completed successfully.",
                        ButtonType.OK);
                ok.setHeaderText("Setup Complete");
                ok.showAndWait();
                close();
            } catch (Exception ex) {
                showError("Setup failed: " + ex.getMessage());
            }
        });

        setScene(new Scene(grid, 480, 260));
    }

    /**
     * @return true if setup completed successfully.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Show a simple error dialog with the given message.
     */
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Database Setup Error");
        alert.showAndWait();
    }
}
