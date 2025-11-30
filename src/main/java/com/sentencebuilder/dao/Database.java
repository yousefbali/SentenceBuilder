/******************************************************************************
 * Database
 *
 * Central utility class for obtaining JDBC Connections for the Sentence
 * Builder application.
 *
 * Responsibilities:
 *   - Load database configuration (host, database, user, password).
 *   - Cache configuration in memory for fast access.
 *   - Provide getConnection() / get() helpers.
 *   - Offer a simple testConnection() check used at startup.
 *
 * Notes:
 *   - Every call returns a NEW JDBC Connection.
 *   - Callers MUST use try-with-resources or otherwise close connections.
 *   - Connections are NOT thread-safe and must not be shared between threads.
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Static utility for DB access; not intended to be instantiated.
 */
public final class Database {

    /**
     * Cached configuration properties.
     *
     * The precedence for where these come from is implemented in loadConfig():
     *   1) Programmatic configuration via configure(...).
     *   2) External db.properties in ~/.sentencebuilder.
     *   3) Classpath /db.properties (development fallback).
     */
    private static Properties cachedProps;

    private Database() {
        // utility class; no instances
    }

    /**
     * Allow other code (like DatabaseBootstrap) to programmatically configure
     * DB settings at runtime. This is used right after a successful setup run.
     *
     * @param props properties including host, database, user, password.
     */
    public static void configure(Properties props) {
        cachedProps = props;
    }

    /**
     * Clear cached configuration (used after reset).
     * Next call to loadConfig() will re-read from disk or classpath.
     */
    public static void clearCachedConfig() {
        cachedProps = null;
    }

    /**
     * Backwards-compatible method for existing code that calls Database.get().
     * Internally just delegates to getConnection().
     *
     * @return a new JDBC Connection.
     * @throws SQLException if connection cannot be established.
     */
    public static Connection get() throws SQLException {
        return getConnection();
    }

    /**
     * Simple connectivity check used at startup.
     * Opens and closes a connection; if it fails, the caller gets an exception.
     */
    public static void testConnection() throws SQLException {
        try (Connection ignored = getConnection()) {
            // If we got here, the connection was successful.
        }
    }

    /**
     * Core method to obtain a new JDBC Connection based on current config.
     *
     * Reads:
     *   - host      (e.g., "localhost:3306")
     *   - database  (e.g., "sentencegen")
     *   - user      (e.g., "sentencegen")
     *   - password  (DB password)
     *
     * @return a new JDBC Connection to the configured MySQL database.
     * @throws SQLException if the driver fails to connect.
     */
    public static Connection getConnection() throws SQLException {
        Properties props = loadConfig();

        // Host is host:port (e.g., "localhost:3306") for convenience.
        String host = props.getProperty("host", "localhost:3306");
        String db   = props.getProperty("database", "sentencegen");
        String user = props.getProperty("user", "root");
        String pass = props.getProperty("password", "password");

        // Build the JDBC URL. Extra parameters are added to:
        //   - avoid SSL prompts,
        //   - handle public key retrieval,
        //   - set timezone / encoding,
        //   - optimize batch behavior.
        String url = "jdbc:mysql://" + host + "/" + db
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&rewriteBatchedStatements=true";

        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Load configuration from (in order of precedence):
     *   1) cached in-memory props (if already set via configure()),
     *   2) ~/.sentencebuilder/db.properties (user-specific config),
     *   3) classpath /db.properties (fallback for development).
     *
     * This method throws a RuntimeException with a clear message if it
     * cannot find any configuration source.
     */
    private static Properties loadConfig() {
        // 1) Use cached settings if we have them.
        if (cachedProps != null) {
            return cachedProps;
        }

        try {
            Properties props;

            // 2) Check for external config file in the user's home directory.
            if (DbConfig.configExists()) {
                // external config in user's home directory
                props = DbConfig.loadConfig();
            } else {
                // 3) Fallback to classpath db.properties (useful for development).
                try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
                    if (in == null) {
                        throw new IOException(
                                "db.properties not found on classpath and no external config present.");
                    }
                    props = new Properties();
                    props.load(in);
                }
            }

            // Cache for future calls to avoid re-reading from disk.
            cachedProps = props;
            return props;
        } catch (IOException e) {
            // Wrap in a runtime exception with a very clear message so callers
            // don't have to handle IOException everywhere.
            throw new RuntimeException(
                    "Failed to load DB configuration. " +
                    "Ensure db.properties exists either in ~/.sentencebuilder or on the classpath.",
                    e
            );
        }
    }
}
