package org.example.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;




public class Database {
    private static final String URL = "jdbc:sqlite:sentence_builder.db";

    static {
        try (Connection c = get();
             Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS files (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  filename   TEXT NOT NULL,
                  path       TEXT NOT NULL,
                  word_count INTEGER NOT NULL,
                  import_date TEXT NOT NULL
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize DB", e);
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
