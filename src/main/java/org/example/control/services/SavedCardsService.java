package org.example.control.services;

import org.example.control.session.UserContext;
import org.example.dao.SavedCardsRepository;
import org.example.models.dto.Card;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public class SavedCardsService {

    private final SavedCardsRepository savedCardsRepository;
    private final UserContext userContext;

    public SavedCardsService(SavedCardsRepository savedCardsRepository, UserContext userContext) {
        this.savedCardsRepository = savedCardsRepository;
        this.userContext = userContext;
    }

    public Card addCard(String holder, String number, String expiry, String type) {
        validateHolder(holder);
        String normPan = normalizePan(number);
        validatePan(normPan);
        validateExpiry(expiry);
        validateType(type);

        Integer userId = userContext.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("Utente non loggato");

        Optional<Integer> id = savedCardsRepository.insertIfAbsentReturningId(userId, holder, number, expiry, type);
        if (id.isEmpty()) {
            throw new IllegalStateException("Carta già salvata");
        }
        return new Card(id.get(), holder, number, expiry, type);
    }

    public void editCard(int cardId, String holder, String number, String expiry, String type) {
        validateHolder(holder);
        String normPan = normalizePan(number);
        validatePan(normPan);
        validateExpiry(expiry);
        validateType(type);

        Integer userId = userContext.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("Utente non loggato");

        boolean ok = savedCardsRepository.updateCard(cardId, userId, holder, number, expiry, type);
        if (!ok) {
            throw new IllegalStateException("Carta duplicata o non trovata");
        }
        new Card(cardId, holder, number, expiry, type);
    }

    public List<Card> loadCards() {
        Integer userId = userContext.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }

        return savedCardsRepository.findByUser(userId).stream()
                .map(r -> new Card(r.id(), r.holder(), r.number(), r.expiry(), r.type()))
                .toList();
    }

    public boolean deleteCard(int cardId) {
        Integer userId = userContext.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        return savedCardsRepository.deleteById(cardId, userId);
    }

    // VALIDAZIONI E UTILITY

    public static void validateHolder(String holder) {
        if (holder == null || holder.trim().isEmpty())
            throw new IllegalArgumentException("L'intestatario non può essere vuoto.");
    }

    public static String normalizePan(String pan) {
        return pan == null ? "" : pan.replaceAll("\\D", "");
    }

    public static void validatePan(String digits) {
        if (digits.length() < 13 || digits.length() > 19)
            throw new IllegalArgumentException("Il numero carta deve avere tra 13 e 19 cifre.");
        if (!luhnOk(digits))
            throw new IllegalArgumentException("Il numero carta non supera il controllo Luhn.");
    }

    private static boolean luhnOk(String d) {
        int sum = 0;
        boolean alt = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    public static void validateExpiry(String expiry) {
        if (expiry == null) throw new IllegalArgumentException("Inserisci la scadenza (MM/AA).");
        String e = expiry.trim();
        if (!e.matches("^(0[1-9]|1[0-2])/\\d{2}$"))
            throw new IllegalArgumentException("Formato scadenza non valido. Usa MM/AA (es. 12/27).");
        int month = Integer.parseInt(e.substring(0, 2));
        int year = 2000 + Integer.parseInt(e.substring(3, 5));
        YearMonth expYm = YearMonth.of(year, month);
        YearMonth nowYm = YearMonth.now(ZoneId.of("Europe/Rome"));
        if (expYm.isBefore(nowYm))
            throw new IllegalArgumentException("La carta risulta scaduta.");
    }

    public static void validateType(String type) {
        if ("Credito".equals(type) || "Debito".equals(type)) return;
        throw new IllegalArgumentException("Tipo carta non valido. Usa 'Credito' o 'Debito'.");
    }
}
