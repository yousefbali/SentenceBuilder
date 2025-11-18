package com.sentencebuilder.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * IMPORTANT: This returns a NEW JDBC Connection on every call.
 * Do NOT share a single Connection across threads; JDBC Connections are not thread-safe.
 * Always use try-with-resources at call sites.
 */
public class Database {

    public static Connection get() throws SQLException {
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) throw new RuntimeException("Missing db.properties in resources.");
            Properties props = new Properties();
            props.load(in);
            String host = props.getProperty("host","localhost:3306");
            String db = props.getProperty("database","sentencebuilder");
            String user = props.getProperty("user","root");
            String pass = props.getProperty("password","password");
            // Works with MySQL driver against MariaDB
            String url = "jdbc:mysql://" + host + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            return DriverManager.getConnection(url, user, pass);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }
}
