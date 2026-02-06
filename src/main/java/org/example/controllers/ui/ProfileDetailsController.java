package org.example.controllers.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.controllers.app.ProfileDetailsAppController;

public class ProfileDetailsController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Button editBtn;

    private ProfileDetailsAppController appController;

    private boolean editMode = false;

    public void setAppController(ProfileDetailsAppController app) {
        this.appController = app;
        if (app != null) {
            app.loadUserData(this::displayUserData);
        }
    }

    @FXML
    public void initialize() {
        usernameField.setEditable(false);
        passwordField.setEditable(false);
        emailField.setEditable(false);
        phoneField.setEditable(false);
    }

    private void displayUserData(String[] data) {
        usernameField.setText(data[0]);
        passwordField.setText(data[1]);
        emailField.setText(data[2]);
        phoneField.setText(data[3]);
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
            passwordField.setText("");
            return;
        }

        editBtn.setText("Modifica dati");

        String currentUsername = appController.getCurrentUsername();
        String newUsername = usernameField.getText().trim();
        String newEmail    = emailField.getText().trim();
        String newPhone    = phoneField.getText().trim();
        String newPwd      = passwordField.getText();

        appController.updateProfile(currentUsername, newUsername, newEmail, newPhone, newPwd,
                success -> {
                    if (Boolean.TRUE.equals(success)) {
                        passwordField.setText("******");
                        usernameField.setEditable(false);
                        passwordField.setEditable(false);
                        emailField.setEditable(false);
                        phoneField.setEditable(false);
                        showAlert("Profilo aggiornato correttamente.");
                    } else {
                        showAlert("Errore durante l'aggiornamento.");
                    }
                },
                errorMsg -> showAlert("Errore durante l'aggiornamento: " + errorMsg)
        );
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

