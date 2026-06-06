package org.example.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public final class DatabaseConnection {
    private static final String CONFIG_FILE = "/db.properties";

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
        Properties p = new Properties();
        try (InputStream in = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile leggere " + CONFIG_FILE, e);
        }
        return p;
    }

    public static Connection getConnection() throws SQLException {
        final String url;
        final String user;
        final String pwd;

        if (urlOverride != null) {
            url = urlOverride;
            user = userOverride;
            pwd = passwordOverride;
        } else {
            Properties p = props();

            url = firstNonBlank(
                    System.getProperty("db.url"),
                    System.getenv("DB_URL"),
                    p.getProperty("db.url")
            );

            user = firstNonBlank(
                    System.getProperty("db.user"),
                    System.getenv("DB_USER"),
                    p.getProperty("db.user")
            );

            pwd = firstNonBlank(
                    System.getProperty("db.password"),
                    System.getenv("DB_PASSWORD"),
                    p.getProperty("db.password")
            );
        }

        if (isBlank(url) || isBlank(user) || isBlank(pwd)) {
            throw new IllegalStateException(
                    "Configurazione database mancante. " +
                            "Imposta db.url, db.user, db.password tramite " +
                            "System properties, variabili d'ambiente o file db.properties."
            );
        }

        try {
            return DriverManager.getConnection(url, user, pwd);
        } catch (SQLException ex) {
            throw new SQLException("Connessione DB fallita (url=" + url + ", user=" + user + ")", ex);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}