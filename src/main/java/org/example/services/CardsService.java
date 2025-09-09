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

// Servizio per le carte salvate
public final class CardsService {
    private CardsService(){}

    private static final Logger LOGGER = Logger.getLogger(CardsService.class.getName());

    // Risultato dell’add inline
    public enum AddCardStatus { ADDED, DUPLICATE, VALIDATION_ERROR, ERROR }

    public record AddCardResult(AddCardStatus status, String message, Card card) {
        public boolean ok() { return status == AddCardStatus.ADDED; }
    }

    // Dati input carta inseriti dall’utente
    private record InlineInputs(String holder, String number, String expiry, String type) {}

    // Riferimenti UI per aggiornare la schermata
    private record UiRefs(
            ComboBox<String> typeCombo,
            ObservableList<Card> cards,
            TableView<Card> table,
            TextField holderField,
            TextField numberField,
            TextField expiryField
    ) {}

    public static void loadSavedCards(int userId, ObservableList<Card> target) throws SQLException {
        target.clear();
        List<SavedCardsDAO.Row> rows = SavedCardsDAO.findByUser(userId);
        for (SavedCardsDAO.Row r : rows) {
            target.add(new Card(r.getId(), r.getHolder(), r.getCardNumber(), r.getExpiry(), r.getCardType()));
        }
    }

    // Aggiunge una carta se non presente e aggiorna la TableView.
    public static void addInlineCard(
            int userId,
            TextField holderField, TextField numberField, TextField expiryField,
            ComboBox<String> typeCombo,
            ObservableList<Card> cards,
            TableView<Card> table
    ) {
        // Legge input
        InlineInputs inputs = new InlineInputs(
                safe(holderField),
                safe(numberField),
                safe(expiryField),
                (typeCombo != null) ? typeCombo.getValue() : null
        );
        UiRefs ui = new UiRefs(typeCombo, cards, table, holderField, numberField, expiryField);

        // Validazione
        String validationError = validateInlineInputs(inputs);
        if (validationError != null) {
            new AddCardResult(AddCardStatus.VALIDATION_ERROR, validationError, null);
            return;
        }

        // Inserisce e aggiorna UI
        insertCardAndUpdateUI(userId, inputs, ui);
    }

    // Helpers

    private static String validateInlineInputs(InlineInputs in) {
        if (isBlank(in.holder) || isBlank(in.number) || isBlank(in.expiry) || isBlank(in.type)) {
            return "Compila tutti i campi (titolare, numero, scadenza e tipo).";
        }
        if (in.number.replaceAll(CardUi.DIGITS_ONLY_REGEX, "").length() < 12) {
            return "Compila correttamente il Numero (min 12 cifre).";
        }
        if (!in.expiry.matches("^\\d{2}/\\d{2}$")) {
            return "Compila correttamente la Scadenza (MM/YY).";
        }
        return null;
    }

    private static void insertCardAndUpdateUI(int userId, InlineInputs in, UiRefs ui) {
        try {
            Optional<Integer> maybeId = SavedCardsDAO.insertIfAbsentReturningId(
                    userId, in.holder, in.number, in.expiry, in.type
            );
            if (maybeId.isPresent()) {
                Card c = new Card(maybeId.get(), in.holder, in.number, in.expiry, in.type);

                // aggiorna lista + selezione
                ui.cards.addFirst(c);
                if (ui.table != null) ui.table.getSelectionModel().select(c);

                // pulisci form
                clear(ui.holderField);
                clear(ui.numberField);
                clear(ui.expiryField);
                if (ui.typeCombo != null) ui.typeCombo.setValue(CardUi.CARD_TYPE_DEBITO);

                new AddCardResult(AddCardStatus.ADDED, "Carta aggiunta correttamente.", c);
                return;
            }
            new AddCardResult(AddCardStatus.DUPLICATE, "Carta già presente.", null);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore salvataggio carta", e);
            new AddCardResult(AddCardStatus.ERROR, "Errore durante il salvataggio della carta.", null);
        }
    }

    private static String safe(TextField tf) {
        return (tf == null || tf.getText() == null) ? "" : tf.getText().trim();
    }
    private static void clear(TextField tf) {
        if (tf != null) tf.clear();
    }
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
