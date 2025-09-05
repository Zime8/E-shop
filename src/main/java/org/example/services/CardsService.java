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
import java.util.logging.Level;
import java.util.logging.Logger;

/** Servizio per le carte salvate. */
public final class CardsService {
    private CardsService(){}

    private static final Logger LOGGER = Logger.getLogger(CardsService.class.getName());

    /** Risultato dell’add inline. */
    public enum AddCardStatus { ADDED, DUPLICATE, VALIDATION_ERROR, ERROR }

    public record AddCardResult(AddCardStatus status, String message, Card card) {
        public boolean ok() { return status == AddCardStatus.ADDED; }
    }

    public static void loadSavedCards(int userId, ObservableList<Card> target) throws SQLException {
        target.clear();
        List<SavedCardsDAO.Row> rows = SavedCardsDAO.findByUser(userId);
        for (SavedCardsDAO.Row r : rows) {
            target.add(new Card(r.getId(), r.getHolder(), r.getCardNumber(), r.getExpiry(), r.getCardType()));
        }
    }

    /**
     * Aggiunge una carta “inline” (se non presente) e aggiorna la TableView.
     * Restituisce un risultato con esito e messaggio da mostrare.
     *
     * @return AddCardResult: status + message + card (se creata)
     */
    public static AddCardResult addInlineCard(
            int userId,
            TextField holderField, TextField numberField, TextField expiryField,
            ComboBox<String> typeCombo,
            ObservableList<Card> cards,
            TableView<Card> table
    ) {
        String holder = safe(holderField);
        String number = safe(numberField);
        String expiry = safe(expiryField);
        String type   = (typeCombo != null) ? typeCombo.getValue() : null;

        // Validazioni base
        if (holder.isEmpty() || number.isEmpty() || expiry.isEmpty() || type == null || type.isBlank()) {
            return new AddCardResult(AddCardStatus.VALIDATION_ERROR,
                    "Compila tutti i campi (titolare, numero, scadenza e tipo).", null);
        }
        if (number.replaceAll(CardUi.DIGITS_ONLY_REGEX, "").length() < 12) {
            return new AddCardResult(AddCardStatus.VALIDATION_ERROR,
                    "Compila correttamente il Numero (min 12 cifre).", null);
        }
        if (!expiry.matches("^\\d{2}/\\d{2}$")) {
            return new AddCardResult(AddCardStatus.VALIDATION_ERROR,
                    "Compila correttamente la Scadenza (MM/YY).", null);
        }

        try {
            Optional<Integer> maybeId = SavedCardsDAO.insertIfAbsentReturningId(userId, holder, number, expiry, type);
            if (maybeId.isPresent()) {
                Card c = new Card(maybeId.get(), holder, number, expiry, type);
                // aggiorna UI list & selezione
                cards.addFirst(c);
                if (table != null) table.getSelectionModel().select(c);
                // pulisci form
                if (holderField != null) holderField.clear();
                if (numberField != null) numberField.clear();
                if (expiryField != null) expiryField.clear();
                if (typeCombo != null) typeCombo.setValue(CardUi.CARD_TYPE_DEBITO);

                return new AddCardResult(AddCardStatus.ADDED, "Carta aggiunta correttamente.", c);
            } else {
                return new AddCardResult(AddCardStatus.DUPLICATE, "Carta già presente.", null);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore salvataggio carta", e);
            return new AddCardResult(AddCardStatus.ERROR, "Errore durante il salvataggio della carta.", null);
        }
    }

    private static String safe(TextField tf) {
        return (tf == null || tf.getText() == null) ? "" : tf.getText().trim();
    }
}
