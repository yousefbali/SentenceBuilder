/******************************************************************************
 * ResetWizard
 *
 * Modal dialog used to completely reset the Sentence Builder application:
 *   - Drops the sentencegen database and application user.
 *   - Deletes the local db.properties configuration.
 *
 * After a successful reset, the application will close so the user
 * can restart with a clean setup.
 *
 * Written by Yousef Ali  for CS 4485.0W1,
 * starting November 7, 2025.
 ******************************************************************************/

package com.sentencebuilder.ui;

import com.sentencebuilder.dao.DatabaseBootstrap;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Simple, modal wizard to perform a full app reset via DatabaseBootstrap.
 */
public class ResetWizard extends Stage {

    /** True if the reset completed successfully. */
    private boolean success = false;

    /**
     * Construct the reset wizard.
     *
     * @param owner owner stage (for modality).
     */
    public ResetWizard(Stage owner) {
        setTitle("Reset Application - Sentence Builder");
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("3306");
        TextField userField = new TextField("root");
        PasswordField passField = new PasswordField();

        CheckBox confirmBox = new CheckBox(
                "I understand this will drop and recreate the database."
        );

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

        Label warningLabel = new Label(
                "Warning: This will drop the existing Sentence Builder database\n" +
                "and app user, and remove local DB configuration."
        );
        grid.add(warningLabel, 0, row, 2, 1);

        grid.add(confirmBox, 0, ++row, 2, 1);

        Button cancelBtn = new Button("Cancel");
        Button resetBtn = new Button("Reset");

        HBox buttons = new HBox(8, cancelBtn, resetBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttons, 0, ++row, 2, 1);

        // Behavior
        cancelBtn.setOnAction(e -> {
            success = false;
            close();
        });

        resetBtn.setOnAction(e -> {
            if (!confirmBox.isSelected()) {
                showError("You must check the confirmation box to proceed.");
                return;
            }

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
                // Perform the destructive reset via DatabaseBootstrap.
                DatabaseBootstrap.resetApp(host, port, adminUser, adminPass);
                success = true;

                Alert ok = new Alert(
                        Alert.AlertType.INFORMATION,
                        "Application reset successfully.\n\nThe app will now close.",
                        ButtonType.OK
                );
                ok.setHeaderText("Reset Complete");
                ok.showAndWait();

                // Close the wizard itself.
                close();

                // Close the primary stage (if we have one) and exit JavaFX.
                Stage primary = (Stage) getOwner();
                if (primary != null) {
                    primary.close();
                }
                Platform.exit();

            } catch (Exception ex) {
                showError("Reset failed: " + ex.getMessage());
            }
        });

        setScene(new Scene(grid, 520, 260));
    }

    /**
     * @return true if the reset operation completed successfully.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Show an error dialog with the given message.
     */
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Reset Error");
        alert.showAndWait();
    }
}

