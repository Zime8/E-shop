package org.example.controllers.app;
import org.example.control.services.PaymentSelectionService;
import org.example.gateway.PaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CardViewModel;
import org.example.models.CartItem;

import java.math.BigDecimal;
import java.util.List;

public class PaymentSelectionAppController {
    private final PaymentSelectionService service;

    public PaymentSelectionAppController(PaymentGateway gateway) {
        this.service = new PaymentSelectionService(gateway);
    }

    public List<CardViewModel> loadSavedCards(int userId) {
        return service.loadSavedCards(userId);
    }

    public PaymentResult confirmPayment(Card card, String cvv, String address,
                                        List<CartItem> items, BigDecimal total) {
        return service.confirmPayment(card, cvv, address, items, total);
    }

    public String getLastOrderIds() {
        return service.getLastOrderIds();
    }
}
