package org.example.control.services;

import org.example.dao.SavedCardsRepository;
import org.example.models.dto.Card;
import org.example.models.dto.InlineCardData;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CardsService {

    private static final Logger LOGGER = Logger.getLogger(CardsService.class.getName());
    private final SavedCardsRepository savedCardsRepository;

    public CardsService(SavedCardsRepository savedCardsRepository) {
        this.savedCardsRepository = savedCardsRepository;
    }

    // Risultato dell’add inline
    public enum AddCardStatus { ADDED, DUPLICATE, VALIDATION_ERROR, ERROR }

    public record AddCardResult(AddCardStatus status, String message, Card card) {
        public boolean ok() { return status == AddCardStatus.ADDED; }
    }

    public List<Card> loadSavedCards(int userId) {
        return savedCardsRepository.findByUser(userId).stream()
                .map(r -> new Card(r.id(), r.holder(), r.number(), r.expiry(), r.type()))
                .toList();
    }

    public AddCardResult addInlineCard(int userId, InlineCardData data) {
        // Validazione
        String validationError = validateInlineData(data);
        if (validationError != null) {
            return new AddCardResult(AddCardStatus.VALIDATION_ERROR, validationError, null);
        }

        try {
            Optional<Integer> maybeId = savedCardsRepository.insertIfAbsentReturningId(
                    userId, data.holder(), data.number(), data.expiry(), data.type());

            if (maybeId.isPresent()) {
                Card card = new Card(maybeId.get(), data.holder(), data.number(), data.expiry(), data.type());
                return new AddCardResult(AddCardStatus.ADDED, "Carta aggiunta!", card);
            }
            return new AddCardResult(AddCardStatus.DUPLICATE, "Carta già presente.", null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore salvataggio carta", e);
            return new AddCardResult(AddCardStatus.ERROR, "Errore salvataggio.", null);
        }
    }

    // Helpers

    private String validateInlineData(InlineCardData data) {
        if (isBlank(data.holder()) || isBlank(data.number()) || isBlank(data.expiry()) || isBlank(data.type())) {
            return "Compila tutti i campi (titolare, numero, scadenza e tipo).";
        }
        String digits = data.number().replaceAll("\\D", "");
        if (digits.length() < 12) {
            return "Compila correttamente il Numero (min 12 cifre).";
        }
        if (!data.expiry().matches("^\\d{2}/\\d{2}$")) {
            return "Compila correttamente la Scadenza (MM/YY).";
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
