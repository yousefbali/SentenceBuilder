/******************************************************************************
 * DbConfig
 *
 * Small utility class for reading and writing the external database
 * configuration file used by Sentence Builder.
 *
 * The configuration file lives at:
 *   db.properties in the application working directory
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
 * Simple helper for working with db.properties in the application
 * working directory.
 */
/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
public final class DbConfig {

    // Config file path, for example:
    //   <project-root>/db.properties
    // or, when running a JAR:
    //   <folder-where-you-run-the-JAR>/db.properties
    private static final Path CONFIG_FILE =
            Paths.get("db.properties");

    private DbConfig() {
        // utility class; no instances
    }

    /**
     * @return true if ./db.properties currently exists.
     */
    public static boolean configExists() {
        return Files.exists(CONFIG_FILE);
    }

    /**
     * Load configuration from ./db.properties into a Properties object.
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
     * Save configuration to ./db.properties.
     * Creates the parent directory if needed (in case a relative subdirectory
     * is used).
     *
     * @param props properties to write (host, database, user, password, etc.).
     * @throws IOException if writing fails.
     */
    public static void saveConfig(Properties props) throws IOException {
        // Ensure parent directory exists before writing (usually null for plain "db.properties").
        Path parent = CONFIG_FILE.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = new FileOutputStream(CONFIG_FILE.toFile())) {
            // store(...) writes Java-style properties with an optional comment header.
            props.store(out, "SentenceBuilder DB configuration");
        }
    }

    /**
     * Delete the ./db.properties file if it exists.
     *
     * @throws IOException if deletion fails in an unexpected way.
     */
    public static void deleteConfig() throws IOException {
        Files.deleteIfExists(CONFIG_FILE);
    }

    /**
     * Expose the resolved path for tooling, debugging, or UI messages.
     *
     * @return Path to ./db.properties.
     */
    public static Path getConfigFilePath() {
        return CONFIG_FILE;
    }
}

