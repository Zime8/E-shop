package org.example.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public final class DatabaseConnection {
    private static final String CONFIG_FILE = "/db.properties";

    // Override per test
    private static volatile String urlOverride;
    private static volatile String userOverride;
    private static volatile String passwordOverride;

    private static volatile Properties cachedProps;

    private DatabaseConnection() {}

    public static synchronized void override(String url, String user, String password) {
        urlOverride = url;
        userOverride = user;
        passwordOverride = password;
    }

    public static synchronized void clearOverride() {
        urlOverride = null;
        userOverride = null;
        passwordOverride = null;
    }

    // Carica e restituisce le proprietà del file db.properties
    private static Properties props() {
        Properties p = cachedProps;
        if (p == null) {
            synchronized (DatabaseConnection.class) {
                if (cachedProps == null) cachedProps = loadConfigProperties();
                p = cachedProps;
            }
        }
        return p;
    }

    private static Properties loadConfigProperties() {
        try (InputStream in = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in == null) throw new IllegalStateException("Config non trovata: " + CONFIG_FILE);
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile leggere " + CONFIG_FILE, e);
        }
    }

    // Apre una nuova Connection. Usa try-with-resources lato chiamante
    public static Connection getConnection() throws SQLException{
        final String url;
        final String user;
        final String pwd;

        if (urlOverride != null) {
            url = urlOverride; user = userOverride; pwd = passwordOverride;
        } else {
            Properties p = props();
            url = System.getProperty("db.url", p.getProperty("db.url"));
            user = System.getProperty("db.user", p.getProperty("db.user"));
            pwd = System.getProperty("db.password", p.getProperty("db.password"));
        }
        try {
            return DriverManager.getConnection(url, user, pwd);
        } catch (SQLException ex) {
            throw new SQLException("Connessione DB fallita (url=" + url + ", user=" + user + ")", ex);
        }
    }
}
