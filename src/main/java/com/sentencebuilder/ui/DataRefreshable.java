/******************************************************************************
 * DataRefreshable
 *
 * Simple contract for UI panes that depend on database-backed data and need
 * a way to refresh that data when the underlying tables change.
 *
 * Typical usage:
 *   - AnalyticsPane: reload summary stats and top words.
 *   - DatabaseViewerPane: reset paging and reload current table.
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.ui;

/**
 * Interface for panes that should reload data after operations such as
 * importing new files or clearing/resetting the database.
 */
public interface DataRefreshable {

    /**
     * Reload any data this pane depends on from the database.
     * Implementations should be careful not to block the JavaFX Application
     * Thread with long-running operations.
     */
    void refreshData();
}
