package org.example.services;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.dao.SavedCardsDAO;
import org.example.models.Card;
import org.example.ui.CardUi;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static Optional<Card> addInlineCard(
            int userId,
            TextField holderField, TextField numberField, TextField expiryField, ComboBox<String> typeCombo,
            ObservableList<Card> cards, TableView<Card> table,
            Consumer<String> onInfo, Consumer<String> onError, Logger logger
    ) {
        String holder = safe(holderField);
        String number = safe(numberField);
        String expiry = safe(expiryField);
        String type   = (typeCombo != null) ? typeCombo.getValue() : null;

        if (holder.isEmpty() || number.isEmpty() || expiry.isEmpty() || type == null || type.isBlank()) {
            onInfo.accept("Compila tutti i campi (titolare, numero, scadenza e tipo).");
            return Optional.empty();
        }
        if (number.replaceAll(CardUi.DIGITS_ONLY_REGEX, "").length() < 12) {
            onInfo.accept("Compila correttamente il Numero (min 12 cifre).");
            return Optional.empty();
        }
        if (!expiry.matches("^\\d{2}/\\d{2}$")) {
            onInfo.accept("Compila correttamente la Scadenza (MM/YY).");
            return Optional.empty();
        }

        try {
            Optional<Integer> maybeId = SavedCardsDAO.insertIfAbsentReturningId(userId, holder, number, expiry, type);
            if (maybeId.isPresent()) {
                Card c = new Card(maybeId.get(), holder, number, expiry, type);
                cards.addFirst(c);                           // in cima
                if (table != null) table.getSelectionModel().select(c);
                if (holderField != null) holderField.clear();
                if (numberField != null) numberField.clear();
                if (expiryField != null) expiryField.clear();
                typeCombo.setValue(CardUi.CARD_TYPE_DEBITO);
                onInfo.accept("Carta aggiunta correttamente.");
                return Optional.of(c);
            } else {
                onInfo.accept("Carta già presente.");
                return Optional.empty();
            }
        } catch (Exception e) {
            if (logger != null) logger.log(Level.SEVERE, "Errore salvataggio carta", e);
            onError.accept("Errore durante il salvataggio della carta.");
            return Optional.empty();
        }
    }

    private static String safe(TextField tf) {
        return (tf == null || tf.getText() == null) ? "" : tf.getText().trim();
    }

}
