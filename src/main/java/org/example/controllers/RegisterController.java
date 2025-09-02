package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.dao.UserDAO;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegisterController {

    private static final Logger logger = Logger.getLogger(RegisterController.class.getName());
    @FXML private TextField phoneField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void onRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String role = "cliente";

        if (userDAO.isUsernameTaken(username)){
            showAlert("L'username è già esistente.");
            return;
        }

        if (userDAO.isEmailTaken(email)){
            showAlert("L'email è già esistente.");
            return;
        }

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty() || phone.isEmpty()) {
            showAlert("Per favore, compila tutti i campi.");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert("Le password non corrispondono.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showAlert("Inserisci un'email valida.");
            return;
        }

        if (!phone.matches("^\\d{7,12}$")) {
            showAlert("Inserisci un numero di telefono valido (7-12 cifre).");
            return;
        }

        boolean success = userDAO.registerUser(username, password, role, email, phone);

        if (success) {
            showAlert("Registrazione completata!");

            // Torna al login
            onBack();
        } else {
            showAlert("Errore durante la registrazione. Username già esistente o problema nel database.");
        }
    }

    @FXML
    private void onBack() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore nel caricamento della schermata di login", e);
            showAlert("Errore durante il caricamento della schermata di login.");
        }
    }


    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
