package org.example.controllers.control;

import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CardViewModel;
import org.example.models.CartItem;
import org.example.models.InlineCardData;
import java.math.BigDecimal;
import java.util.List;

public interface PaymentSelectionControl {
    List<CardViewModel> loadSavedCards();
    CardViewModel addInlineCard(InlineCardData data);
    PaymentResult confirmPayment(Card card, String cvv, String address, List<CartItem> items, BigDecimal total);
    String getLastOrderIds();
}

