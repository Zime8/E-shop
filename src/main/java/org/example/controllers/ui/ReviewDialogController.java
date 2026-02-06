package org.example.controllers.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.controllers.app.ReviewDialogAppController;

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

    public void setController(ReviewDialogAppController appController){
        this.appController = appController;
        if (appController != null) {
            appController.setupStars(star1, star2, star3, star4, star5, ratingLabel);
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

    private void close() {
        Stage st = (Stage) root.getScene().getWindow();
        st.close();
    }

    private void showWarning(String msg){
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}
