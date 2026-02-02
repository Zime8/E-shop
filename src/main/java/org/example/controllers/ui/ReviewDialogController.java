package org.example.controllers.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.controllers.app.ReviewDialogAppController;
import org.example.models.Product;

public class ReviewDialogController {

    @FXML private StackPane root;
    @FXML private Label productTitle;
    @FXML private ToggleButton star1;
    @FXML private ToggleButton star2;
    @FXML private ToggleButton star3;
    @FXML private ToggleButton star4;
    @FXML private ToggleButton star5;
    @FXML private Label ratingLabel;
    @FXML private TextField titleField;
    @FXML private TextArea commentArea;

    private ReviewDialogAppController appController;

    @FXML
    private void initialize() {
    }

    public void setController(ReviewDialogAppController appController){
        this.appController = appController;
        appController.setupStars(star1, star2, star3, star4, star5, ratingLabel);
    }

    public void init(Product product) {
        appController.init(product, productTitle);
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
