package org.example.boundary;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.control.RegisterAppController;
import org.example.models.RegisterValidationResult;
import org.example.util.Navigator;

import java.util.logging.Logger;

public class RegisterController {

    private static final Logger logger = Logger.getLogger(RegisterController.class.getName());

    @FXML private TextField phoneField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;

    private RegisterAppController appController;
    private Navigator navigator;

    public void setAppController(RegisterAppController app) {
        this.appController = app;
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML public void initialize() {
        Platform.runLater(() -> usernameField.requestFocus());
        phoneField.setPromptText("es. 3331234567");
        emailField.setPromptText("es. user@example.com");
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

    @FXML
    private void onBack() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        navigator.goToLogin(stage);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.initOwner(usernameField.getScene().getWindow());
        alert.showAndWait();
    }
}
