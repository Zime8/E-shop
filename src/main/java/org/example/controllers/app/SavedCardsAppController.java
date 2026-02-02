package org.example.controllers.app;

import org.example.controllers.control.SavedCardsControl;
import org.example.dao.SavedCardsDAO;
import org.example.models.Card;
import org.example.util.Session;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SavedCardsAppController implements SavedCardsControl {

    private static final Logger logger = Logger.getLogger(SavedCardsAppController.class.getName());

    @Override
    public Card addCard(String holder, String number, String expiry, String type) {
        validateHolder(holder);
        String normPan = normalizePan(number);
        validatePan(normPan);
        validateExpiry(expiry);
        validateType(type);

        Integer userId = Session.getUserId();
        if (userId == null) throw new IllegalStateException("Utente non loggato");

        try {
            Optional<Integer> id = SavedCardsDAO.insertIfAbsentReturningId(userId, holder, number, expiry, type);
            if (id.isEmpty()) {
                throw new IllegalStateException("Carta già salvata");
            }
            return new Card(id.get(), holder, number, expiry, type);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore: ", e);
            throw new IllegalStateException("Errore salvataggio carta");
        }
    }

    @Override
    public Card editCard(int cardId, String holder, String number, String expiry, String type) {
        validateHolder(holder);
        String normPan = normalizePan(number);
        validatePan(normPan);
        validateExpiry(expiry);
        validateType(type);

        Integer userId = Session.getUserId();
        if (userId == null) throw new IllegalStateException("Utente non loggato");

        try {
            boolean ok = SavedCardsDAO.updateCard(cardId, userId, holder, number, expiry, type);
            if (!ok) {
                throw new IllegalStateException("Carta duplicata o non trovata");
            }
            return new Card(cardId, holder, number, expiry, type);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore aggiornamento carta ID=" + cardId, e);
            throw new IllegalStateException("Errore database: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Card> loadCards() {
        try {
            Integer userId = Session.getUserId();
            if (userId == null) return List.of();
            return SavedCardsDAO.findByUser(userId).stream()
                    .map(r -> new Card(r.id(), r.holder(), r.cardNumber(), r.expiry(), r.cardType()))
                    .toList();  // ✅ Immutabili
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore caricamento carte", e);
            return List.of();
        }
    }

    @Override
    public boolean deleteCard(int cardId) {
        try {
            Integer userId = Session.getUserId();
            if (userId == null) return false;
            return SavedCardsDAO.deleteById(cardId, userId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore eliminazione carta", e);
            return false;
        }
    }

    // VALIDAZIONI E UTILITY (invariati + safe callback)

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
        YearMonth nowYm = YearMonth.now();
        if (expYm.isBefore(nowYm))
            throw new IllegalArgumentException("La carta risulta scaduta.");
    }

    public static void validateType(String type) {
        if ("Credito".equals(type) || "Debito".equals(type)) return;
        throw new IllegalArgumentException("Tipo carta non valido. Usa 'Credito' o 'Debito'.");
    }

    public static String maskPan(String raw) {
        if (raw == null) return "";
        String d = raw.replaceAll("\\D", "");
        if (d.length() <= 4) return d;
        String last4 = d.substring(d.length() - 4);
        return "**** **** **** " + last4;
    }
}
