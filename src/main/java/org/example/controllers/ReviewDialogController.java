package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.models.Product;

import java.util.Optional;

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

    private int rating = 5;           // default
    private boolean confirmed = false;

    @FXML
    private void initialize() {
        // userData = indice stella
        star1.setUserData(1);
        star2.setUserData(2);
        star3.setUserData(3);
        star4.setUserData(4);
        star5.setUserData(5);

        // Assicura la style class "star" e gestisci la "on" quando cambia selected
        setupStar(star1);
        setupStar(star2);
        setupStar(star3);
        setupStar(star4);
        setupStar(star5);

        // stato iniziale (5/5)
        setRating(5);
    }

    public void init(Product product) {
        String shopName = product.getNameShop() != null ? product.getNameShop() : "";
        productTitle.setText(product.getName() + (shopName.isBlank() ? "" : " • " + shopName));
        setRating(5);
    }

    @FXML
    private void onStarClick(ActionEvent e) {
        ToggleButton src = (ToggleButton) e.getSource();
        Object ud = src.getUserData();
        int r = (ud instanceof Integer i) ? i : 1;
        setRating(r);
    }

    private void setRating(int r) {
        this.rating = clamp(r);

        star1.setSelected(rating >= 1);
        star2.setSelected(rating >= 2);
        star3.setSelected(rating >= 3);
        star4.setSelected(rating >= 4);
        star5.setSelected(rating >= 5);

        ratingLabel.setText(rating + "/5");
        // Le classi "on" vengono aggiornate automaticamente dai listener in setupStar(...)
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        close();
    }

    @FXML
    private void onSave() {
        if (rating < 1 || rating > 5) {
            new Alert(Alert.AlertType.WARNING, "Il voto deve essere tra 1 e 5.").showAndWait();
            return;
        }
        confirmed = true;
        close();
    }

    private void close() {
        Stage st = (Stage) root.getScene().getWindow();
        st.close();
    }

    public Optional<ReviewData> getResult() {
        if (!confirmed) return Optional.empty();
        return Optional.of(new ReviewData(
                rating,
                emptyToNull(titleField.getText()),
                emptyToNull(commentArea.getText())
        ));
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static int clamp(int v) {
        return (v < 1) ? 1 : (Math.min(v, 5));
    }

    // ---------- gestione stile stelle (classe .on al posto di :selected) ----------
    private void setupStar(ToggleButton tb) {
        if (!tb.getStyleClass().contains("star")) tb.getStyleClass().add("star");
        // Sync iniziale
        updateStarClass(tb, tb.isSelected());
        // Aggiorna quando cambia selected
        tb.selectedProperty().addListener((obs, was, isSel) -> updateStarClass(tb, isSel));
    }

    private void updateStarClass(ToggleButton tb, boolean on) {
        if (on) {
            if (!tb.getStyleClass().contains("on")) tb.getStyleClass().add("on");
        } else {
            tb.getStyleClass().remove("on");
        }
    }

    /** DTO */
    public record ReviewData(int rating, String title, String comment) {}
}
