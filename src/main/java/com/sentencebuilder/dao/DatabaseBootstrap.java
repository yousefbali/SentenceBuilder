/******************************************************************************
 * DatabaseBootstrap
 *
 * One-time / administrative operations for setting up or resetting the
 * Sentence Builder MySQL database.
 *
 * Responsibilities:
 *   - runSetup(host, port, adminUser, adminPass):
 *       * Connect to MySQL as an admin.
 *       * Execute schema.sql to create the sentencegen DB + tables.
 *       * Create a dedicated low-privilege app user.
 *       * Grant privileges to that app user on the sentencegen DB.
 *       * Save db.properties via DbConfig for the app to use later.
 *
 *   - resetApp(host, port, adminUser, adminPass):
 *       * Drop the sentencegen DB and app user (if they exist).
 *       * Delete local db.properties.
 *       * Clear in-memory DB configuration cache.
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Static utility for bootstrapping and resetting the application database.
 */
public class DatabaseBootstrap {

    private DatabaseBootstrap() {
        // utility class; no instances
    }

    /**
     * First-time setup:
     *   - Connect to MySQL as an admin user (root or similar).
     *   - Execute schema.sql (creates sentencegen DB and tables).
     *   - Create restricted application user 'sentencegen'@'localhost'.
     *   - Grant that user privileges on the sentencegen database.
     *   - Persist db.properties for the app to use later.
     *
     * @param host      MySQL host (e.g., "localhost").
     * @param port      MySQL port (e.g., 3306).
     * @param adminUser Admin username (e.g., "root").
     * @param adminPass Admin password.
     */
    public static void runSetup(String host,
                                int port,
                                String adminUser,
                                String adminPass) throws Exception {

        // Admin URL does not specify a database; schema.sql will create it.
        String adminUrl = String.format(
                "jdbc:mysql://%s:%d/?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false",
                host, port);

        try (Connection conn = DriverManager.getConnection(adminUrl, adminUser, adminPass)) {

            // 1. Run schema.sql from classpath (creates sentencegen DB and tables).
            runSchemaSql(conn);

            // 2. Create dedicated app user (local-only for now).
            String appUser = "sentencegen";
            String appPass = "sentencegen";

            try (Statement stmt = conn.createStatement()) {
                // CREATE USER IF NOT EXISTS 'sentencegen'@'localhost' IDENTIFIED BY 'sentencegen';
                stmt.executeUpdate(
                        "CREATE USER IF NOT EXISTS '" + appUser + "'@'localhost' IDENTIFIED BY '" + appPass + "'"
                );

                // Grant the app user privileges on the sentencegen database.
                stmt.executeUpdate(
                        "GRANT ALL PRIVILEGES ON `sentencegen`.* TO '" + appUser + "'@'localhost'"
                );

                // Ensure privilege changes are picked up.
                stmt.executeUpdate("FLUSH PRIVILEGES");
            }

            // 3. Save config that the rest of the app will use later.
            Properties props = new Properties();
            props.setProperty("host", host + ":" + port);
            props.setProperty("database", "sentencegen");
            props.setProperty("user", appUser);
            props.setProperty("password", appPass);

            // Persist to ~/.sentencebuilder/db.properties.
            DbConfig.saveConfig(props);

            // Also push this config into memory so this run can use it immediately.
            Database.configure(props);
        }
    }

    /**
     * Execute schema.sql on the provided admin Connection.
     *
     * schema.sql is expected to live on the classpath at /schema.sql and to
     * contain valid MySQL statements separated by semicolons.
     *
     * Implementation outline:
     *   - Read the entire file into a string.
     *   - Split on ';' to get individual statements.
     *   - Trim and execute non-empty statements.
     */
    private static void runSchemaSql(Connection conn) throws IOException, SQLException {
        // Load schema.sql from the classpath.
        try (InputStream in = DatabaseBootstrap.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql not found on classpath");
            }

            // Read all lines into a single string (preserving newlines for readability).
            String sql = new BufferedReader(new InputStreamReader(in))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Naive split on ';' is sufficient for this assignment-level schema.
            String[] statements = sql.split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String raw : statements) {
                    String s = raw.trim();
                    if (!s.isEmpty()) {
                        // Execute each statement individually so failures are easier to localize.
                        stmt.execute(s);
                    }
                }
            }
        }
    }

    /**
     * Wipes the app DB + user + local config. Intended to be followed
     * by another call to runSetup(...) to recreate everything from scratch.
     *
     * @param host      MySQL host.
     * @param port      MySQL port.
     * @param adminUser Admin username.
     * @param adminPass Admin password.
     */
    public static void resetApp(String host,
                                int port,
                                String adminUser,
                                String adminPass) throws Exception {

        String adminUrl = String.format(
                "jdbc:mysql://%s:%d/?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false",
                host, port);

        // Use admin account to drop DB + user.
        try (Connection conn = DriverManager.getConnection(adminUrl, adminUser, adminPass);
             Statement stmt = conn.createStatement()) {

            // 1. Drop database and app user if they exist.
            stmt.executeUpdate("DROP DATABASE IF EXISTS `sentencegen`");
            stmt.executeUpdate("DROP USER IF EXISTS 'sentencegen'@'localhost'");
            stmt.executeUpdate("FLUSH PRIVILEGES");
        }

        // 2. Delete local config file and clear in-memory config cache.
        try {
            DbConfig.deleteConfig();
        } catch (IOException e) {
            // Not fatal; log and continue. The app can still recreate config later.
            e.printStackTrace();
        }

        // Ensure Database will reload configuration next time.
        Database.clearCachedConfig();
    }
}
