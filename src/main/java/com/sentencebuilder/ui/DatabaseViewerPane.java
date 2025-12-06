/******************************************************************************
 * DatabaseViewerPane
 *
 * UI pane for inspecting the contents of the Sentence Builder database.
 * It supports:
 *   - Quickly switching between tables (files, words, bigrams, trigrams, etc.).
 *   - Paging through rows with a fixed PAGE_SIZE.
 *   - Optionally showing "friendly" word text instead of raw IDs for
 *     relation tables (bigrams/trigrams/word_files).
 *   - Clearing all application data from the database.
 *
 * Written by Yousef Ali (yba210001) for CS 4485.0W1,
 * starting October 23, 2025.
 ******************************************************************************/

package com.sentencebuilder.ui;

import com.sentencebuilder.dao.Database;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX pane that acts as a lightweight DB viewer for the Sentence Builder schema.
 */
public class DatabaseViewerPane extends BorderPane implements DataRefreshable {

    /** Number of rows to display per page. */
    private static final int PAGE_SIZE = 500;

    /** Timestamp format for columns like import_date (view-only, DB unchanged). */
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("MMM d, uuuu HH:mm:ss");

    /**
     * Descriptor for tables the viewer knows about.
     * Left value is the label shown in the UI; right value is the actual table name.
     */
    private static final String[][] TABLE_INFOS = {
            {"Files",      "files"},
            {"Words",      "words"},
            {"Bigrams",    "word_relations_bigram"},
            {"Trigrams",   "word_relations_trigram"},
            {"Word Files", "word_files"},
            {"Metadata",   "metadata"}
    };

    /** Toggle group controlling which logical table is currently selected. */
    private final ToggleGroup tableToggleGroup = new ToggleGroup();

    /** Row of pill-style toggle buttons for table selection. */
    private final HBox tableToggleRow = new HBox(8);

    /** FX table used to render rows from the selected database table. */
    private final TableView<List<String>> table = new TableView<>();

    /** Current page index (0-based). */
    private int currentPage = 0;

    /** Label that shows page info (page and total rows). */
    private final Label pageInfoLabel = new Label("No data loaded");

    /** Button for flipping to the previous page. */
    private final Button prevPageBtn = new Button("Prev");

    /** Button for flipping to the next page. */
    private final Button nextPageBtn = new Button("Next");

    /** Button that toggles "friendly" word display for relation tables. */
    private final Button toggleViewBtn = new Button();

    /** Container for the friendly/raw toggle (we hide/show this as needed). */
    private final HBox viewControlsRow;

    /** True if relation tables should show word text instead of raw IDs. */
    private boolean showFriendlyWords = true;

    /**
     * Construct a new DatabaseViewerPane and build the UI.
     */
    public DatabaseViewerPane() {
        // Page background
        getStyleClass().add("page-root");

        // Header
        Label title = new Label("Database viewer");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label(
                "Inspect the underlying database tables, page through rows, and clear data when needed."
        );
        subtitle.getStyleClass().add("page-subtitle");

        VBox headerBox = new VBox(4, title, subtitle);

        // Table selection row
        Label tablesLabel = new Label("Tables");
        tablesLabel.getStyleClass().add("section-title");

        for (String[] info : TABLE_INFOS) {
            String label = info[0];
            String tableName = info[1];

            ToggleButton btn = new ToggleButton(label);
            btn.getStyleClass().add("toggle-pill");
            btn.setUserData(tableName);
            btn.setToggleGroup(tableToggleGroup);

            tableToggleRow.getChildren().add(btn);
        }
        tableToggleRow.getStyleClass().add("toolbar-row");

        // Friendly vs raw IDs toggle; only visible for relation tables.
        toggleViewBtn.getStyleClass().add("button-secondary");
        updateToggleButtonLabel();

        viewControlsRow = new HBox(toggleViewBtn);
        viewControlsRow.setAlignment(Pos.CENTER_RIGHT);
        viewControlsRow.getStyleClass().add("toolbar-row");
        viewControlsRow.managedProperty().bind(viewControlsRow.visibleProperty());

        // Table
        table.setPlaceholder(new Label("No rows to display."));
        VBox.setVgrow(table, Priority.ALWAYS);

        // Pagination + clear button at the bottom
        prevPageBtn.setDisable(true);
        nextPageBtn.setDisable(true);

        Button clearBtn = new Button("Clear all app data...");
        clearBtn.getStyleClass().add("button-danger");

        // spacer to push the clear button to the right
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox paginationRow = new HBox(8, prevPageBtn, nextPageBtn, pageInfoLabel, spacer, clearBtn);
        paginationRow.setAlignment(Pos.CENTER_LEFT);
        paginationRow.getStyleClass().add("toolbar-row");

        // Card content
        VBox content = new VBox(
                10,
                headerBox,
                new Separator(),
                tablesLabel,
                tableToggleRow,
                viewControlsRow,
                new Separator(),
                table,
                new Separator(),
                paginationRow
        );
        content.getStyleClass().add("page-card");
        content.setPadding(new Insets(0));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        setCenter(scroll);
        BorderPane.setMargin(scroll, new Insets(0, 0, 16, 0));

        // Behavior

        // When the user switches tables, reset pagination and reload,
        // and show/hide the friendly-view toggle as appropriate.
        tableToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            currentPage = 0;
            updateToggleVisibility();
            refresh();
        });

        prevPageBtn.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                refresh();
            }
        });

        nextPageBtn.setOnAction(e -> {
            currentPage++;
            refresh();
        });

        toggleViewBtn.setOnAction(e -> {
            // Flip between "raw ID" view and "friendly word text" view
            // for the relation tables.
            showFriendlyWords = !showFriendlyWords;
            updateToggleButtonLabel();
            refresh();
        });

        clearBtn.setOnAction(e -> clearDatabase());

        // Select first toggle by default and load data.
        if (!tableToggleGroup.getToggles().isEmpty()) {
            tableToggleGroup.getToggles().get(0).setSelected(true);
        }
        updateToggleVisibility();
        refresh();
    }

    /**
     * Helper to get the currently selected table name from the toggle group.
     */
    private String getSelectedTableName() {
        Toggle toggle = tableToggleGroup.getSelectedToggle();
        if (toggle == null) {
            return null;
        }
        return (String) toggle.getUserData();
    }

    /**
     * Update the text and tooltip on the "friendly view" toggle button.
     */
    private void updateToggleButtonLabel() {
        if (showFriendlyWords) {
            toggleViewBtn.setText("Show Raw IDs");
            toggleViewBtn.setTooltip(new Tooltip(
                    "Show raw ID columns instead of joined word text."
            ));
        } else {
            toggleViewBtn.setText("Show Friendly Words");
            toggleViewBtn.setTooltip(new Tooltip(
                    "For relation tables, join IDs against the words table."
            ));
        }
    }

    /**
     * Show or hide the friendly-view toggle row depending on which table is active.
     * We show it for:
     *   - word_relations_bigram
     *   - word_relations_trigram
     *   - word_files
     */
    private void updateToggleVisibility() {
        String tableName = getSelectedTableName();
        boolean isRelationTable =
                "word_relations_bigram".equalsIgnoreCase(tableName) ||
                "word_relations_trigram".equalsIgnoreCase(tableName) ||
                "word_files".equalsIgnoreCase(tableName);

        viewControlsRow.setVisible(isRelationTable);
    }

    /**
     * Refresh the table contents based on the selected table and current page.
     * This method:
     *   1) Builds a base SELECT (possibly with JOINs).
     *   2) Applies LIMIT/OFFSET for paging.
     *   3) Rebuilds the TableView columns dynamically from ResultSet metadata.
     */
    private void refresh() {
        String tableName = getSelectedTableName();
        if (tableName == null) {
            table.getItems().clear();
            table.getColumns().clear();
            pageInfoLabel.setText("No table selected.");
            prevPageBtn.setDisable(true);
            nextPageBtn.setDisable(true);
            return;
        }

        try (Connection conn = Database.get();
             Statement stmt = conn.createStatement(
                     ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY)) {

            // Base query (may include joins if showFriendlyWords is enabled).
            String querySql = buildQueryForTable(tableName);

            int offset = currentPage * PAGE_SIZE;
            String pageSql = querySql + " LIMIT " + PAGE_SIZE + " OFFSET " + offset;

            ResultSet rs = stmt.executeQuery(pageSql);
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            // Build rows into a list so we can compute column widths before populating the TableView.
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= cols; i++) {
                    Object obj = rs.getObject(i);

                    String value;
                    if (obj == null) {
                        value = "null";
                    } else if (obj instanceof Timestamp ts) {
                        // Format timestamps (e.g., import_date) in a more readable form.
                        value = ts.toLocalDateTime().format(TIMESTAMP_FMT);
                    } else {
                        value = obj.toString();
                    }
                    row.add(value);
                }
                rows.add(row);
            }

            // Compute max text length for each column (header + values) to size columns reasonably.
            int[] maxCharsPerCol = new int[cols];
            for (int i = 0; i < cols; i++) {
                String header = md.getColumnLabel(i + 1);
                maxCharsPerCol[i] = header == null ? 0 : header.length();
            }
            for (List<String> row : rows) {
                for (int i = 0; i < cols; i++) {
                    String v = row.get(i);
                    if (v != null && v.length() > maxCharsPerCol[i]) {
                        maxCharsPerCol[i] = v.length();
                    }
                }
            }

            // Rebuild TableView columns from metadata.
            table.getColumns().clear();
            for (int i = 1; i <= cols; i++) {
                final int colIndex = i - 1;
                String header = md.getColumnLabel(i);

                TableColumn<List<String>, String> col = new TableColumn<>(header);
                col.setCellValueFactory(cell -> {
                    List<String> row = cell.getValue();
                    String value = colIndex < row.size() ? row.get(colIndex) : "";
                    return new SimpleStringProperty(value);
                });

                int chars = maxCharsPerCol[colIndex];
                col.setPrefWidth(20 + chars * 7); // very rough heuristic
                table.getColumns().add(col);
            }

            table.getItems().setAll(rows);

            // Update pagination labels and enable/disable navigation buttons.
            int rowCount = countRows(conn, querySql);
            int totalPages = (int) Math.ceil((double) rowCount / PAGE_SIZE);

            prevPageBtn.setDisable(currentPage <= 0);
            nextPageBtn.setDisable(currentPage >= totalPages - 1);

            // No timestamp here; just page and total rows.
            pageInfoLabel.setText(
                    "Page " + (currentPage + 1) + " of " + Math.max(totalPages, 1) +
                            " — " + rowCount + " rows total"
            );

        } catch (Exception ex) {
            table.getItems().clear();
            table.getColumns().clear();
            pageInfoLabel.setText("Error loading table: " + ex.getMessage());
            new Alert(AlertType.ERROR,
                    "Error loading table: " + ex.getMessage()
            ).showAndWait();
        }
    }

    /**
     * Build a SELECT statement for the given table, optionally joining to the
     * words table when friendly word display is enabled.
     *
     * Column names are chosen to match ImportService:
     *   - Bigrams: word_relations_bigram(from_word_id, to_word_id, frequency)
     *   - Trigrams: word_relations_trigram(first_word_id, second_word_id, next_word_id, frequency)
     *   - Word Files: word_files(word_id, file_id, count_in_file)
     *
     * @param tableName base table to view.
     * @return SQL string that can be extended with LIMIT/OFFSET.
     */
    private String buildQueryForTable(String tableName) {
        // For the words table, or if we are not showing friendly words,
        // we simply SELECT * directly.
        if ("words".equalsIgnoreCase(tableName) || !showFriendlyWords) {
            return "SELECT * FROM " + tableName;
        }

        // Friendly bigram view: show the word text instead of numeric IDs.
        if ("word_relations_bigram".equalsIgnoreCase(tableName)) {
            return "SELECT " +
                   "  br.from_word_id, " +
                   "  w1.word_text AS from_word, " +
                   "  br.to_word_id, " +
                   "  w2.word_text AS to_word, " +
                   "  br.frequency " +
                   "FROM word_relations_bigram br " +
                   "JOIN words w1 ON br.from_word_id = w1.word_id " +
                   "JOIN words w2 ON br.to_word_id   = w2.word_id";
        }

        // Friendly trigram view: show each of the three words plus the frequency.
        if ("word_relations_trigram".equalsIgnoreCase(tableName)) {
            return "SELECT " +
                   "  tr.first_word_id, " +
                   "  w1.word_text AS first_word, " +
                   "  tr.second_word_id, " +
                   "  w2.word_text AS second_word, " +
                   "  tr.next_word_id, " +
                   "  w3.word_text AS next_word, " +
                   "  tr.frequency " +
                   "FROM word_relations_trigram tr " +
                   "JOIN words w1 ON tr.first_word_id  = w1.word_id " +
                   "JOIN words w2 ON tr.second_word_id = w2.word_id " +
                   "JOIN words w3 ON tr.next_word_id   = w3.word_id";
        }

        // Friendly word_files view: show word text alongside the IDs.
        if ("word_files".equalsIgnoreCase(tableName)) {
            return "SELECT " +
                   "  wf.word_id, " +
                   "  w.word_text AS word, " +
                   "  wf.file_id, " +
                   "  wf.count_in_file " +
                   "FROM word_files wf " +
                   "JOIN words w ON wf.word_id = w.word_id";
        }

        // Default: no JOIN, just view the table as-is.
        return "SELECT * FROM " + tableName;
    }

    /**
     * Count total rows for a given base query (without LIMIT/OFFSET).
     */
    private int countRows(Connection conn, String baseSql) throws Exception {
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * Action handler for the "Clear all app data..." button.
     * Asks the user to confirm before truncating tables.
     */
    private void clearDatabase() {
        Alert confirm = new Alert(
                AlertType.CONFIRMATION,
                "This will delete all application data from the database " +
                "(files, words, n-grams, and metadata).\n\n" +
                "Are you sure you want to proceed?",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        confirm.setHeaderText("Confirm Clear");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                doClearDatabase();
            }
        });
    }

    /**
     * Actually truncate tables to clear app data, and then refresh this pane.
     */
    private void doClearDatabase() {
        try (Connection c = Database.get();
             Statement stmt = c.createStatement()) {

            // These match the tables that ImportService writes into.
            stmt.executeUpdate("TRUNCATE TABLE word_relations_bigram");
            stmt.executeUpdate("TRUNCATE TABLE word_relations_trigram");
            stmt.executeUpdate("TRUNCATE TABLE word_files");
            stmt.executeUpdate("TRUNCATE TABLE words");
            stmt.executeUpdate("TRUNCATE TABLE files");
            // Metadata is intentionally left in place.

            currentPage = 0;
            refresh();

        } catch (Exception ex) {
            Alert error = new Alert(
                    AlertType.ERROR,
                    "Failed to clear database: " + ex.getMessage(),
                    ButtonType.OK
            );
            error.setHeaderText("Error");
            error.showAndWait();
        }
    }

    /**
     * Reset paging and reload the current table; used when the underlying
     * data changes (e.g., after an import).
     */
    @Override
    public void refreshData() {
        currentPage = 0;
        refresh();
    }
}

