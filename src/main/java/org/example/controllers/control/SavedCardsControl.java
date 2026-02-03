package org.example.controllers.control;

import org.example.models.Card;

import java.util.List;

public interface SavedCardsControl {
    List<Card> loadCards();
    Card addCard(String holder, String number, String expiry, String type);
    void editCard(int cardId, String holder, String number, String expiry, String type);
    boolean deleteCard(int cardId);
}

