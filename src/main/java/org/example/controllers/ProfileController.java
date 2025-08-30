package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ProfileController {

    @FXML private Button profileDetailsBtn;
    @FXML private Button purchaseHistoryBtn;
    @FXML private Button savedCardsBtn;

    private Runnable onProfileDetails;
    private Runnable onPurchaseHistory;
    private Runnable onSavedCards;

    public void initialize() {
        profileDetailsBtn.setOnAction(e -> {
            if (onProfileDetails != null) onProfileDetails.run();
        });
        purchaseHistoryBtn.setOnAction(e -> {
            if (onPurchaseHistory != null) onPurchaseHistory.run();
        });
        savedCardsBtn.setOnAction(e -> {
            if (onSavedCards != null) onSavedCards.run();
        });
    }

    public void setOnProfileDetails(Runnable action) {
        this.onProfileDetails = action;
    }

    public void setOnPurchaseHistory(Runnable action) {
        this.onPurchaseHistory = action;
    }

    public void setOnSavedCards(Runnable action) {
        this.onSavedCards = action;
    }
}
