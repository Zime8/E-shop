package org.example.boundary;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.control.LoginAppController;
import org.example.models.dto.LoginResult;
import org.example.util.Navigator;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());
    private static final double LOGIN_WIDTH = 600;
    private static final double LOGIN_HEIGHT = 500;

    @FXML private Button loginButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private LoginAppController appController;
    private Navigator navigator;

    public void setAppController(LoginAppController app) {
        this.appController = app;
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML public void initialize() {
        Platform.runLater(() -> {
            applyLoginWindowSizing();
            usernameField.requestFocus();
        });
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        if(appController == null){
            showAlert(Alert.AlertType.ERROR, "Controller applicativo non disponibile.");
            return;
        }
        if (navigator == null) {
            showAlert(Alert.AlertType.ERROR, "Navigazione non disponibile.");
            return;
        }

        LoginResult res = appController.performLogin(user, pass);

        switch (res.status()) {
            case SUCCESS -> {
                passwordField.clear();
                Stage stage = (Stage) loginButton.getScene().getWindow();
                navigator.goAfterLogin(stage, res);
            }
            case INVALID_INPUT -> {
                passwordField.clear();
                showAlert(Alert.AlertType.WARNING, "Inserisci username e password.");
            }
            case INVALID_CREDENTIALS -> {
                passwordField.clear();
                showAlert(Alert.AlertType.WARNING, "Username o password non corretti.");
            }
            case ERROR -> showAlert(Alert.AlertType.ERROR, "Si è verificato un errore. Riprova.");
        }
    }

    @FXML
    private void onRegisterLink() {
        navigate();
    }

    @FXML
    private void onDemoMode() {
        if (appController == null) {
            showAlert(Alert.AlertType.ERROR, "Controller applicativo non disponibile.");
            return;
        }
        if (navigator == null) {
            showAlert(Alert.AlertType.ERROR, "Navigazione non disponibile.");
            return;
        }

        try {
            appController.performDemoLogin();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            navigator.goToBuyerHome(stage);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Errore demo", e);
            showAlert(Alert.AlertType.ERROR, "Impossibile avviare la modalità demo.");
        }
    }

    private void navigate() {
        if (navigator == null) {
            showAlert(Alert.AlertType.ERROR, "Navigazione non disponibile.");
            return;
        }
        Stage stage = (Stage) loginButton.getScene().getWindow();
        navigator.goToRegister(stage);
    }

    private void applyLoginWindowSizing() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setMaximized(false);
        stage.setResizable(true);
        stage.setWidth(LOGIN_WIDTH);
        stage.setHeight(LOGIN_HEIGHT);
        stage.centerOnScreen();
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        if (loginButton != null && loginButton.getScene() != null) {
            alert.initOwner(loginButton.getScene().getWindow());
        }
        alert.showAndWait();
    }
}
