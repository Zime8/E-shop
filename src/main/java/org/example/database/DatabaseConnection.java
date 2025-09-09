package org.example.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static volatile Connection connection;
    private static final String CONFIG_FILE = "/db.properties";
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    // --- campi override per i test ---
    private static volatile String urlOverride;
    private static volatile String userOverride;
    private static volatile String passwordOverride;

    private DatabaseConnection() {}

    /**
     * Forza credenziali/URL per i test (es. Testcontainers).
     * Chiude l'eventuale connessione aperta così la prossima getInstance() riapre con questi parametri.
     */
    public static synchronized void override(String url, String user, String password) {
        urlOverride = url;
        userOverride = user;
        passwordOverride = password;
        closeConnection(); // assicura ricreazione con i nuovi parametri
    }

    /** Rimuove l’override (torna a usare db.properties / system properties). */
    public static synchronized void clearOverride() {
        urlOverride = null;
        userOverride = null;
        passwordOverride = null;
        closeConnection();
    }

    public static synchronized Connection getInstance() throws SQLException {
        try {
            if (connection != null && !connection.isClosed()) {
                // Se è ancora valida, riusa
                try {
                    if (connection.isValid(2)) return connection;
                } catch (SQLException ignore) {
                    // se fallisce il check, chiudi e riapri
                    closeConnection();
                }
            }

            String url;
            String user;
            String password;

            if (urlOverride != null) {
                // Parametri imposti dai test
                url = urlOverride;
                user = userOverride;
                password = passwordOverride;
            } else {
                // Carica da file e consenti override via System properties
                Properties props = new Properties();
                try (InputStream input = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
                    if (input == null) {
                        throw new IllegalStateException("File di configurazione non trovato: " + CONFIG_FILE);
                    }
                    props.load(input);
                } catch (IOException e) {
                    throw new IllegalStateException("Impossibile leggere il file di configurazione", e);
                }

                url = System.getProperty("db.url", props.getProperty("db.url"));
                user = System.getProperty("db.user", props.getProperty("db.user"));
                password = System.getProperty("db.password", props.getProperty("db.password"));
            }

            connection = DriverManager.getConnection(url, user, password);
            return connection;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Errore apertura connessione DB", ex);
            throw ex;
        }
    }

    public static synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante la chiusura della connessione", e);
        }
    }
}
