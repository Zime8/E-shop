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

    private static Connection connection;
    private static final String CONFIG_FILE = "/db.properties";
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    private DatabaseConnection() {}

    public static Connection getInstance() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Properties props = new Properties();

            try (InputStream input = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
                if (input == null) {
                    throw new IllegalStateException("File di configurazione non trovato: " + CONFIG_FILE);
                }
                props.load(input);
            } catch (IOException e) {
                throw new IllegalStateException("Impossibile leggere il file di configurazione");
            }

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");

            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    public static void closeConnection() {
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
