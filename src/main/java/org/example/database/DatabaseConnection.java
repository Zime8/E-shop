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

    // campi override per i test
    private static volatile String urlOverride;
    private static volatile String userOverride;
    private static volatile String passwordOverride;

    private DatabaseConnection() {}

    // Forza credenziali/URL per i test
    public static synchronized void override(String url, String user, String password) {
        urlOverride = url;
        userOverride = user;
        passwordOverride = password;
        closeConnection(); // assicura ricreazione con i nuovi parametri
    }

    // Rimuove l’override (torna a usare db.properties / system properties)
    public static synchronized void clearOverride() {
        urlOverride = null;
        userOverride = null;
        passwordOverride = null;
        closeConnection();
    }

    private static boolean isReusable(Connection c) {
        try {
            return c != null && !c.isClosed() && c.isValid(2);
        } catch (SQLException ignore) {
            closeConnection();
            return false;
        }
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

    public static synchronized Connection getInstance() throws SQLException {
        String url;
        String user;
        try {
            if (isReusable(connection)) return connection;

            String password;

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

            connection = DriverManager.getConnection(url, user, password);
            return connection;
        } catch (SQLException ex) {
            throw new SQLException(
                    "Impossibile aprire la connessione DB", ex
            );
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
