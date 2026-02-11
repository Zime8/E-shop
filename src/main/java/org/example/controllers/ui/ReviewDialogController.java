package org.example.controllers.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.controllers.app.ReviewDialogAppController;
import org.example.models.Review;
import org.example.util.Session;

import java.time.LocalDateTime;

public class ReviewDialogController {

    @FXML private StackPane root;
    @FXML private ToggleButton star1;
    @FXML private ToggleButton star2;
    @FXML private ToggleButton star3;
    @FXML private ToggleButton star4;
    @FXML private ToggleButton star5;
    @FXML private Label ratingLabel;
    @FXML private TextField titleField;
    @FXML private TextArea commentArea;

    private ReviewDialogAppController appController;

    public void setAppController(Object appController){
        this.appController = (ReviewDialogAppController) appController;
        if (appController != null) {
            ((ReviewDialogAppController) appController).setupStars(star1, star2, star3, star4, star5, ratingLabel);
        }
    }

    @FXML
    public void initialize() {
        titleField.clear();
        commentArea.clear();
        titleField.requestFocus();
    }

    @FXML
    private void onStarClick(ActionEvent e) {
        ToggleButton src = (ToggleButton) e.getSource();
        appController.onStarSelected(src, star1, star2, star3, star4, star5, ratingLabel);
    }

    @FXML
    private void onCancel() {
        appController.onCancel(this::close);
    }

    @FXML
    private void onSave() {
        appController.onSave(titleField.getText(), commentArea.getText(), this::close, this::showWarning);
    }

    public Review getResult() {
        // Trova rating attivo
        int rating = 0;
        if (star5.isSelected()) rating = 5;
        else if (star4.isSelected()) rating = 4;
        else if (star3.isSelected()) rating = 3;
        else if (star2.isSelected()) rating = 2;
        else if (star1.isSelected()) rating = 1;

        if (rating == 0) {
            showWarning("Seleziona un voto (stelle)!");
            return null;  // Cancella
        }

        Integer userId = appController.findCurrentUserId();
        if (userId == null) {
            showWarning("Utente non trovato!");
            return null;
        }

        return new Review(
                userId,
                Session.getUser(),
                rating,
                titleField.getText().trim(),
                commentArea.getText().trim(),
                LocalDateTime.now()
        );
    }


    private void close() {
        Stage st = (Stage) root.getScene().getWindow();
        st.close();
    }

    private void showWarning(String msg){
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}
