package org.example.controllers.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.controllers.app.RegisterAppController;
import org.example.controllers.app.RegisterAppController.RegisterValidationResult;

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

    private RegisterAppController appController;

    @FXML public void initialize() {
        appController = new RegisterAppController();
    }

    @FXML private void onRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String role = "cliente";

        RegisterValidationResult result = appController.validateAndRegister(
                username, password, confirm, email, phone, role
        );

        switch (result) {
            case SUCCESS -> {
                showAlert("Registrazione completata!");
                onBack();
            }
            case EMPTY_FIELDS -> showAlert("Per favore, compila tutti i campi.");
            case PASSWORD_MISMATCH -> showAlert("Le password non corrispondono.");
            case INVALID_EMAIL -> showAlert("Inserisci un'email valida.");
            case INVALID_PHONE -> showAlert("Inserisci un numero di telefono valido (7-12 cifre).");
            case USERNAME_TAKEN -> showAlert("L'username è già esistente.");
            case EMAIL_TAKEN -> showAlert("L'email è già esistente.");
            case DATABASE_ERROR -> showAlert("Errore durante la registrazione. Username già esistente o problema nel database.");
        }
    }

    @FXML private void onBack() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento login", e);
            showAlert("Errore durante il caricamento della schermata di login.");
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.initOwner(usernameField.getScene().getWindow());
        alert.showAndWait();
    }
}
