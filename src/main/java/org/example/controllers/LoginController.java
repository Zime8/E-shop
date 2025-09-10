package org.example.controllers;

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
import org.example.dao.UserDAO;
import org.example.demo.DemoData;
import org.example.util.Session;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());
    private static final double LOGIN_WIDTH = 600;
    private static final double LOGIN_HEIGHT = 500;

    @FXML private Button loginButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML public void initialize() {
        Session.setDemo(false);
        Platform.runLater(this::applyLoginWindowSizing);
    }

    @FXML
    private void onLogin() {

        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Inserisci username e password.");
            return;
        }

        UserDAO.LoginResult res = UserDAO.checkLogin(user, pass);

        switch (res.status()) {
            case SUCCESS -> {
                Session.setUser(user);
                Session.setUserId(res.userId());
                passwordField.clear();

                String role = res.role();
                if ("venditore".equalsIgnoreCase(role)) {
                    goSellerHome();
                } else {
                    goHome();
                }
            }
            case INVALID_CREDENTIALS -> {
                passwordField.clear();
                showAlert(Alert.AlertType.ERROR, "Credenziali non valide.");
            }
            case ERROR -> showAlert(Alert.AlertType.ERROR, "Si è verificato un errore. Riprova.");
        }
    }

    @FXML
    private void onRegisterLink() {
        navigate("/fxml/Register.fxml",
                "Registrazione",
                false,
                "Errore nel caricamento della schermata Register.fxml",
                "Errore durante il caricamento della schermata per la registrazione.");
    }

    private void goHome() {
        navigate("/fxml/Home.fxml",
                "Home",
                true,
                "Errore nel caricamento della schermata Home.fxml",
                "Errore durante il caricamento della schermata utente.");
    }

    // Apertura area venditore
    private void goSellerHome() {
        navigate("/fxml/SellerHome.fxml",
                "Area Venditore",
                true,
                "Errore nel caricamento della schermata del venditore",
                "Errore durante il caricamento della schermata venditore. ");
    }

    @FXML
    private void onDemoMode() {
        try {
            // Nuova identità guest univoca
            String guest = "ospite-" + java.util.UUID.randomUUID().toString().substring(0, 8);

            int demoId = DemoData.NEXT_DEMO_USER_ID.getAndDecrement();

            // Pulisci sessione precedente
            org.example.util.Session.clear();

            // Attiva demo e setta credenziali guest
            org.example.util.Session.setDemo(true);
            org.example.util.Session.setUser(guest);
            org.example.util.Session.setUserId(demoId);

            org.example.demo.DemoData.ensureLoaded();
            org.example.demo.DemoData.users().putIfAbsent(
                    guest, new org.example.demo.DemoData.User(demoId, guest, null, "utente", null, null)
            );

            goHome();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nell'attivazione della modalità demo", e);
            showAlert(Alert.AlertType.ERROR, "Impossibile avviare la modalità demo.");
        }
    }

    // Metodo che carica FXML, imposta scena/titolo e gestisce errori
    private void navigate(String fxmlPath,
                          String title,
                          boolean maximized,
                          String logContext,
                          String userFacingErrorMsg) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            if (title != null) stage.setTitle(title);
            stage.setMaximized(maximized);
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, logContext, e);
            showAlert(Alert.AlertType.ERROR, userFacingErrorMsg);
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
