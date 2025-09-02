package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.util.Session;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfileDetailsController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Button editBtn;

    private static final Logger logger = Logger.getLogger(ProfileDetailsController.class.getName());

    private boolean editMode = false;

    @FXML
    public void initialize() {

        // Campi non editabili all'avvio
        usernameField.setEditable(false);
        passwordField.setEditable(false);
        emailField.setEditable(false);
        phoneField.setEditable(false);

        loadUserData();
    }

    private void loadUserData() {
        String username = Session.getUser();
        if (username == null) return;

        try {
            var u = UserDAO.findByUsername(username);
            if (u == null) return;

            usernameField.setText(u.getUsername());
            passwordField.setText("******"); // placeholder UI
            emailField.setText(u.getEmail());
            phoneField.setText(u.getPhone());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei dati utente", e);
            showAlert("Errore durante il caricamento dei dati dell'utente: " + username);
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    // gestione della modifica delle informazioni del profilo
    public void onEdit() {
        editMode = !editMode;

        usernameField.setEditable(editMode);
        passwordField.setEditable(editMode);
        emailField.setEditable(editMode);
        phoneField.setEditable(editMode);

        if (editMode) {
            editBtn.setText("Salva");
            passwordField.setText(""); // sblocca per nuovo inserimento
            return;
        }

        // Salvataggio
        editBtn.setText("Modifica dati");

        String current = Session.getUser();
        String newUsername = usernameField.getText().trim();
        String newEmail    = emailField.getText().trim();
        String newPhone    = phoneField.getText().trim();
        String newPwd      = passwordField.getText(); // vuota = non cambiare

        try {
            // (opzionale) validazioni base + univocità
            // if (!newUsername.equals(current) && UserDAO.isUsernameTaken(newUsername)) { ... }

            if (newPwd != null && !newPwd.isBlank()) {
                UserDAO.updateProfileWithPassword(current, newUsername, newEmail, newPhone, newPwd);
            } else {
                UserDAO.updateProfile(current, newUsername, newEmail, newPhone);
            }

            // aggiorna sessione se username cambiato
            Session.setUser(newUsername);

            // UI back to view
            passwordField.setText("******");
            usernameField.setEditable(false);
            passwordField.setEditable(false);
            emailField.setEditable(false);
            phoneField.setEditable(false);

            showAlert("Profilo aggiornato correttamente.");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante l'aggiornamento dei dati utente", e);
            showAlert("Errore durante l'aggiornamento: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

