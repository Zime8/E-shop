package org.example.controllers.app;

import org.example.services.SavedCardsService;
import org.example.dao.UserDAO;
import org.example.models.Card;
import org.example.util.Session;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

public class ModifyProfile {

    private static final String PSW = "******";
    private static final Logger logger = Logger.getLogger(ModifyProfile.class.getName());
    private final SavedCardsService savedCardsService = new SavedCardsService();

    private final UserDAO userDao = new UserDAO();

    public void loadUserData(Consumer<String[]> onDataLoaded) {
        String username = Session.getUser();
        if (username == null) {
            onDataLoaded.accept(new String[]{"", PSW, "", ""});
            return;
        }

        try {
            var u = userDao.findByUsername(username);
            if (u == null) {
                onDataLoaded.accept(new String[]{"", PSW, "", ""});
                return;
            }

            // Prepara dati "sicuri" per UI
            String[] data = {
                    u.getUsername(),
                    PSW,
                    u.getEmail(),
                    u.getPhone()
            };
            onDataLoaded.accept(data);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei dati utente", e);
            onDataLoaded.accept(new String[]{"", "", "", ""});
        }
    }

    public String getCurrentUsername() {
        return Session.getUser();
    }

    public void updateProfile(String currentUsername, String newUsername, String newEmail,
                              String newPhone, String newPwd,
                              Consumer<Boolean> onSuccess, Consumer<String> onError) {
        try {
            if (newPwd != null && !newPwd.isBlank()) {
                userDao.updateProfileWithPassword(currentUsername, newUsername, newEmail, newPhone, newPwd);
            } else {
                userDao.updateProfile(currentUsername, newUsername, newEmail, newPhone);
            }

            // Aggiorna sessione
            Session.setUser(newUsername);

            onSuccess.accept(true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante l'aggiornamento dei dati utente", e);
            onError.accept(e.getMessage());
        }
    }

    public Card addCard(String holder, String number, String expiry, String type) {
        return savedCardsService.addCard(holder, number, expiry, type);
    }

    public List<Card> loadCards() {
        return savedCardsService.loadCards();
    }

    public void editCard(int cardId, String holder, String number, String expiry, String type) {
        savedCardsService.editCard(cardId, holder, number, expiry, type);
    }

    public boolean deleteCard(int cardId) {
        return savedCardsService.deleteCard(cardId);
    }
}

