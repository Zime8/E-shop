package org.example.controllers.app;
import org.example.control.services.PaymentSelectionService;
import org.example.gateway.FakePaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CardViewModel;
import org.example.models.CartItem;

import java.math.BigDecimal;
import java.util.List;

public class PaymentSelectionAppController {
    private final PaymentSelectionService service;
    private Integer userId;

    public Integer getUserId() {
        return userId;
    }

    public PaymentSelectionAppController() {
        this.service = new PaymentSelectionService(new FakePaymentGateway(1000L, 0.10));
    }

    public void loadUserData(int userId) {
        this.userId = userId;
    }

    public List<CardViewModel> loadSavedCards() {
        return service.loadSavedCards(userId);
    }

    public PaymentResult confirmPayment(Card card, String cvv, String address,
                                        List<CartItem> items, BigDecimal total) {
        return service.confirmPayment(card, cvv, address, items, total, userId);
    }

    public String getLastOrderIds() {
        return service.getLastOrderIds();
    }
}
