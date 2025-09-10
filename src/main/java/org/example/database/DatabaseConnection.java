package org.example.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final String CONFIG_FILE = "/db.properties";

    // Campi override per i test
    private static volatile String urlOverride;
    private static volatile String userOverride;
    private static volatile String passwordOverride;

    private DatabaseConnection() {}

    // Forza credenziali/URL per i test
    public static synchronized void override(String url, String user, String password) {
        urlOverride = url;
        userOverride = user;
        passwordOverride = password;
    }

    // Rimuove l’override
    public static synchronized void clearOverride() {
        urlOverride = null;
        userOverride = null;
        passwordOverride = null;
    }

    private static Properties loadConfigProperties() {
        Properties props = new Properties();
        try (InputStream input = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException("File di configurazione non trovato: " + CONFIG_FILE);
            }
            props.load(input);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile leggere il file di configurazione", e);
        }
    }

    // Ritorna una nuova Connection
    public static Connection getInstance() throws SQLException {
        try {
            final String url, user, password;
            if (urlOverride != null) {
                url = urlOverride;
                user = userOverride;
                password = passwordOverride;
            } else {
                Properties props = loadConfigProperties();
                url = System.getProperty("db.url", props.getProperty("db.url"));
                user = System.getProperty("db.user", props.getProperty("db.user"));
                password = System.getProperty("db.password", props.getProperty("db.password"));
            }
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            throw new SQLException("Impossibile aprire la connessione DB", ex);
        }
    }

    public static void closeConnection() {
        // ogni DAO chiude la sua connessione nel try-with-resources
    }
}
