package org.example.control.interfaces;

import org.example.models.Card;

import java.util.List;

public interface SavedCardsControl {
    List<Card> loadCards();
    Card addCard(String holder, String number, String expiry, String type);
    void editCard(int cardId, String holder, String number, String expiry, String type);
    boolean deleteCard(int cardId);

    default String maskPan(String raw) {
        if (raw == null) return "";
        String d = raw.replaceAll("\\D", "");
        if (d.length() <= 4) return d;
        return "**** **** **** " + d.substring(d.length() - 4);
    }
}

