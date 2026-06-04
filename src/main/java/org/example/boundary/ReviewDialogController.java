package org.example.boundary;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.control.ReviewAppController;
import org.example.models.dto.ProductDto;
import org.example.models.dto.ReviewDto;

import java.util.function.Consumer;

public class ReviewDialogController {

    @FXML private StackPane root;
    @FXML private ToggleButton star1;
    @FXML private ToggleButton star2;
    @FXML private ToggleButton star3;
    @FXML private ToggleButton star4;
    @FXML private ToggleButton star5;
    @FXML private Label ratingLabel;
    @FXML private Label productTitle;
    @FXML private TextField titleField;
    @FXML private TextArea commentArea;

    private ReviewAppController appController;
    private ProductDto product;
    private Consumer<ReviewDto> onSaved;
    private Runnable onCancelled;
    private int rating = 5;

    public void setAppController(ReviewAppController appController){
        this.appController = appController;
    }

    public void setProduct(ProductDto product) {
        this.product = product;
        refreshProductTitle();
    }

    public void setOnSaved(Consumer<ReviewDto> onSaved) {
        this.onSaved = onSaved;
    }

    public void setOnCancelled(Runnable onCancelled) {
        this.onCancelled = onCancelled;
    }

    @FXML
    public void initialize() {
        titleField.clear();
        commentArea.clear();
        setupStars();
        refreshProductTitle();
        Platform.runLater(() -> titleField.requestFocus());
    }

    @FXML
    private void onStarClick(ActionEvent e) {
        ToggleButton src = (ToggleButton) e.getSource();
        Object ud = src.getUserData();
        int r = (ud instanceof Integer i) ? i : 1;
        setRating(r);
    }

    @FXML
    private void onCancel() {
        if (onCancelled != null) {
            onCancelled.run();
        }
        close();
    }

    @FXML
    private void onSave() {
        if (appController == null) {
            showWarning("Controller non inizializzato.");
            return;
        }

        try {
            ReviewDto saved = appController.saveReview(
                    product,
                    rating,
                    titleField.getText(),
                    commentArea.getText()
            );

            if (onSaved != null) {
                onSaved.accept(saved);
            }

            close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            showWarning(e.getMessage());
        }
    }

    private void refreshProductTitle() {
        if (productTitle == null) return;

        if (product == null) {
            productTitle.setText("");
            return;
        }

        String productName = product.name();
        String shopName = product.nameShop() != null ? product.nameShop() : "";
        String displayText = productName + (shopName.isBlank() ? "" : " • " + shopName);
        productTitle.setText(displayText);
    }

    private void setupStars() {
        if (star1 == null) return;

        star1.setUserData(1);
        star2.setUserData(2);
        star3.setUserData(3);
        star4.setUserData(4);
        star5.setUserData(5);

        setupStar(star1);
        setupStar(star2);
        setupStar(star3);
        setupStar(star4);
        setupStar(star5);

        setRating(5);
    }

    private void setRating(int r) {
        this.rating = clamp(r);

        star1.setSelected(rating >= 1);
        star2.setSelected(rating >= 2);
        star3.setSelected(rating >= 3);
        star4.setSelected(rating >= 4);
        star5.setSelected(rating >= 5);

        ratingLabel.setText(rating + "/5");
    }

    private void setupStar(ToggleButton tb) {
        if (!tb.getStyleClass().contains("star")) tb.getStyleClass().add("star");
        updateStarClass(tb, tb.isSelected());
        tb.selectedProperty().addListener((obs, was, isSel) -> updateStarClass(tb, isSel));
    }

    private void updateStarClass(ToggleButton tb, boolean on) {
        if (on) {
            if (!tb.getStyleClass().contains("on")) tb.getStyleClass().add("on");
        } else {
            tb.getStyleClass().remove("on");
        }
    }

    private void close() {
        Stage st = (Stage) root.getScene().getWindow();
        st.close();
    }

    private void showWarning(String msg){
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    private static int clamp(int v) {
        return (v < 1) ? 1 : Math.min(v, 5);
    }
}