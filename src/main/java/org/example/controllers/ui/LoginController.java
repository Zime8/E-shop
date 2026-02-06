package org.example.controllers.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.controllers.app.HomeAppController;
import org.example.controllers.app.LoginAppController;
import org.example.controllers.app.LoginAppController.*;
import org.example.controllers.app.RegisterAppController;
import org.example.controllers.app.SellerHomeAppController;

import java.io.IOException;
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

    public void setAppController(LoginAppController app) {
        this.appController = app;
    }

    @FXML public void initialize() {
        Platform.runLater(this::applyLoginWindowSizing);
        usernameField.requestFocus();
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        LoginResult res = appController.performLogin(user, pass);

        switch (res.status()) {
            case SUCCESS -> {
                passwordField.clear();
                navigateToHome(appController.getNextView(res));
            }
            case INVALID_INPUT, INVALID_CREDENTIALS -> {
                passwordField.clear();
                showAlert(Alert.AlertType.WARNING, "Inserisci username e password valide.");
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
        try {
            appController.performDemoLogin();
            navigateToHome("/fxml/Home.fxml");
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Errore demo", e);
            showAlert(Alert.AlertType.ERROR, "Impossibile avviare la modalità demo.");
        }
    }

    private void navigateToHome(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (fxmlPath.contains("SellerHome")) {
                SellerHomeController sellerCtrl = loader.getController();
                SellerHomeAppController sellerApp = new SellerHomeAppController();
                sellerCtrl.setAppController(sellerApp);
            } else {
                HomeController homeCtrl = loader.getController();
                HomeAppController homeApp = new HomeAppController();
                homeCtrl.setAppController(homeApp);
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(fxmlPath.contains("SellerHome") ? "Area Venditore" : "Home");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore Home nav", e);
        }
    }

    private void navigate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
            Parent root = loader.load();

            RegisterController regCtrl = loader.getController();
            regCtrl.setAppController(new RegisterAppController());
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Registrazione");
            stage.setMaximized(false);
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore nel caricamento della schermata Register.fxml", e);
            showAlert(Alert.AlertType.ERROR, "Errore durante il caricamento della schermata per la registrazione.");
        }
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
