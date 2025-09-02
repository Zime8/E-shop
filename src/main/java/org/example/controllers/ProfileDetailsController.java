package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.database.DatabaseConnection;
import org.example.util.Session;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        loadUserData();

        // Campi non editabili all'avvio
        usernameField.setEditable(false);
        passwordField.setEditable(false);
        emailField.setEditable(false);
        phoneField.setEditable(false);
    }

    private void loadUserData() {
        String username = Session.getUser();
        if (username == null) return;

        try (Connection conn = DatabaseConnection.getInstance()) {
            String sql = "SELECT username, email, phone FROM users WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                usernameField.setText(rs.getString("username"));
                passwordField.setText("******");
                emailField.setText(rs.getString("email"));
                phoneField.setText(rs.getString("phone"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei dati utente", e);
            showAlert("Errore durante il caricamento dei dati dell'utente: " + usernameField);
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    // gestione della modifica delle informazioni del profilo
    public void onEdit() {
        editMode = !editMode; // Inverte la modalità

        usernameField.setEditable(editMode);
        passwordField.setEditable(editMode);
        emailField.setEditable(editMode);
        phoneField.setEditable(editMode);

        if (editMode) {
            editBtn.setText("Salva");
            passwordField.setText(""); // Svuota per permettere nuovo inserimento (se vuole cambiare)
        } else {
            editBtn.setText("Modifica dati");
            // Prendi dati dai campi
            String newUsername = usernameField.getText();
            String newPassword = passwordField.getText();
            String newEmail = emailField.getText();
            String newPhone = phoneField.getText();

            // Esegui update solo se i dati sono validi
            updateUserData(newUsername, newPassword, newEmail, newPhone);

            // Torna modalità view
            passwordField.setText("******");
            usernameField.setEditable(false);
            passwordField.setEditable(false);
            emailField.setEditable(false);
            phoneField.setEditable(false);
        }
    }

    private void updateUserData(String username, String password, String email, String phone) {
        String userCorrente = Session.getUser();
        if (userCorrente == null) return;

        try (Connection conn = DatabaseConnection.getInstance()) {
            if (password != null && !password.isEmpty()) {
                String hashedPwd = hashPassword(password); // implementa hashPassword in modo sicuro!
                String sql = "UPDATE users SET username=?, email=?, pass=?, phone=? WHERE username=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, hashedPwd);
                ps.setString(4, phone);
                ps.setString(5, userCorrente);
                ps.executeUpdate();
                ps.close();
            } else {
                String sql = "UPDATE users SET username=?, email=?, phone=? WHERE username=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setString(4, userCorrente);
                ps.executeUpdate();
                ps.close();
            }
            // Aggiorna anche la sessione se username cambiato
            Session.setUser(username);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante l'aggiornamento dei dati utente", e);
            showAlert("Errore durante l'aggiornamento dei dati dell'utente: " + username);
        }
    }

    // Esempio di placeholder per hashPassword
    private String hashPassword(String pwd) {
        return BCrypt.hashpw(pwd, BCrypt.gensalt(12));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

