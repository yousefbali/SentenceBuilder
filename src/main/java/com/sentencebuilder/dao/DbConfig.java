/******************************************************************************
 * DbConfig
 *
 * Small utility class for reading and writing the external database
 * configuration file used by Sentence Builder.
 *
 * The configuration file lives at:
 *   {user.home}/.sentencebuilder/db.properties
 *
 * This stores values such as:
 *   - host=localhost:3306
 *   - database=sentencegen
 *   - user=sentencegen
 *   - password=sentencegen
 *
 * Written by <Your Name> (<Your NetID>) for <Course/Section>, Assignment <N>,
 * starting <Month Day, 2025>.
 ******************************************************************************/

package com.sentencebuilder.dao;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Simple helper for working with ~/.sentencebuilder/db.properties.
 */
public final class DbConfig {

    // Example path (Windows):
    //   C:\Users\<username>\.sentencebuilder\db.properties
    private static final Path CONFIG_DIR =
            Paths.get(System.getProperty("user.home"), ".sentencebuilder");
    private static final Path CONFIG_FILE =
            CONFIG_DIR.resolve("db.properties");

    private DbConfig() {
        // utility class; no instances
    }

    /**
     * @return true if ~/.sentencebuilder/db.properties currently exists.
     */
    public static boolean configExists() {
        return Files.exists(CONFIG_FILE);
    }

    /**
     * Load configuration from ~/.sentencebuilder/db.properties into a Properties object.
     *
     * @return Properties loaded from the config file.
     * @throws IOException if the file cannot be read.
     */
    public static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE.toFile())) {
            props.load(in);
        }
        return props;
    }

    /**
     * Save configuration to ~/.sentencebuilder/db.properties.
     * Creates the directory if needed.
     *
     * @param props properties to write (host, database, user, password, etc.).
     * @throws IOException if writing fails.
     */
    public static void saveConfig(Properties props) throws IOException {
        // Ensure parent directory exists before writing.
        if (!Files.exists(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }
        try (OutputStream out = new FileOutputStream(CONFIG_FILE.toFile())) {
            // store(...) writes Java-style properties with an optional comment header.
            props.store(out, "SentenceBuilder DB configuration");
        }
    }

    /**
     * Delete the ~/.sentencebuilder/db.properties file if it exists.
     *
     * @throws IOException if deletion fails in an unexpected way.
     */
    public static void deleteConfig() throws IOException {
        Files.deleteIfExists(CONFIG_FILE);
    }

    /**
     * Expose the resolved path for tooling, debugging, or UI messages.
     *
     * @return Path to ~/.sentencebuilder/db.properties.
     */
    public static Path getConfigFilePath() {
        return CONFIG_FILE;
    }
}
