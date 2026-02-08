package org.example.controllers.app;

import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import org.example.models.Product;

import java.util.Optional;
import java.util.function.Consumer;

public class ReviewDialogAppController {
    private int rating = 5;
    private boolean confirmed = false;
    private String title;
    private String comment;
    private Product product;

    public ReviewDialogAppController() {
        // Costruttore vuoto
    }

    public ReviewDialogAppController(Product product) {
        this.product = product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setupStars(ToggleButton star1, ToggleButton star2, ToggleButton star3,
                           ToggleButton star4, ToggleButton star5, Label ratingLabel) {
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

        setRating(5, star1, star2, star3, star4, star5, ratingLabel);
    }

    public void init(Label productTitle) {
        String productName = this.product.getName();
        String shopName = this.product.getNameShop() != null ? this.product.getNameShop() : "";
        String displayText = productName + (shopName.isBlank() ? "" : " • " + shopName);
        productTitle.setText(displayText);
    }

    public void onStarSelected(ToggleButton src, ToggleButton star1, ToggleButton star2, ToggleButton star3,
                               ToggleButton star4, ToggleButton star5, Label ratingLabel) {
        Object ud = src.getUserData();
        int r = (ud instanceof Integer i) ? i : 1;
        setRating(r, star1, star2, star3, star4, star5, ratingLabel);
    }

    public void onCancel(Runnable closeAction) {
        confirmed = false;
        closeAction.run();
    }

    public void onSave(String titleText, String commentText, Runnable onSuccess, Consumer<String> onWarning) {
        if (rating < 1 || rating > 5) {
            onWarning.accept("Il voto deve essere tra 1 e 5.");
            return;
        }
        this.title = titleText;
        this.comment = commentText;
        confirmed = true;
        onSuccess.run();
    }

    public Optional<ReviewData> getResult() {
        if (!confirmed) return Optional.empty();
        return Optional.of(new ReviewData(rating, emptyToNull(title), emptyToNull(comment)));
    }

    private void setRating(int r, ToggleButton s1, ToggleButton s2, ToggleButton s3,
                           ToggleButton s4, ToggleButton s5, Label ratingLabel) {
        this.rating = clamp(r);

        s1.setSelected(rating >= 1);
        s2.setSelected(rating >= 2);
        s3.setSelected(rating >= 3);
        s4.setSelected(rating >= 4);
        s5.setSelected(rating >= 5);

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

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static int clamp(int v) {
        return (v < 1) ? 1 : Math.min(v, 5);
    }

    public record ReviewData(int rating, String title, String comment) {}
}
