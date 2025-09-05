package org.example.services;

import javafx.collections.ObservableList;
import org.example.dao.SavedCardsDAO;
import org.example.models.Card;

import java.sql.SQLException;
import java.util.List;

/** Servizio per caricare le carte salvate. */
public final class CardsService {
    private CardsService(){}

    public static void loadSavedCards(int userId, ObservableList<Card> target) throws SQLException {
        target.clear();
        List<SavedCardsDAO.Row> rows = SavedCardsDAO.findByUser(userId);
        for (SavedCardsDAO.Row r : rows) {
            target.add(new Card(r.getId(), r.getHolder(), r.getCardNumber(), r.getExpiry(), r.getCardType()));
        }
    }
}
