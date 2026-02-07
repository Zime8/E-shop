package org.example.control.interfaces;

import org.example.models.Card;
import org.example.models.CardViewModel;

import java.math.BigDecimal;
import java.util.List;

public interface WithdrawSelectionControl {
    BigDecimal loadBalance();  // Ritorna saldo
    List<CardViewModel> loadSavedCards();  // Ritorna carte
    void addInlineCard(String holder, String number, String expiry, String type);
    void confirmWithdraw(BigDecimal amount, Card card, String cvv);
}

