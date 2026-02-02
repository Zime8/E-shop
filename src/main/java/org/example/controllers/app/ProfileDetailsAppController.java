package org.example.controllers.app;

import org.example.dao.UserDAO;
import org.example.util.Session;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

public class ProfileDetailsAppController {

    private static final String PSW = "******";
    private static final Logger logger = Logger.getLogger(ProfileDetailsAppController.class.getName());

    private final UserDAO userDAO = UserDAO.getInstance();

    public void loadUserData(Consumer<String[]> onDataLoaded) {
        String username = Session.getUser();
        if (username == null) {
            onDataLoaded.accept(new String[]{"", PSW, "", ""});
            return;
        }

        try {
            var u = userDAO.findByUsername(username);
            if (u == null) {
                onDataLoaded.accept(new String[]{"", PSW, "", ""});
                return;
            }

            // ← Prepara dati "sicuri" per UI (maschera password)
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
                userDAO.updateProfileWithPassword(currentUsername, newUsername, newEmail, newPhone, newPwd);
            } else {
                userDAO.updateProfile(currentUsername, newUsername, newEmail, newPhone);
            }

            // Aggiorna sessione
            Session.setUser(newUsername);

            onSuccess.accept(true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante l'aggiornamento dei dati utente", e);
            onError.accept(e.getMessage());
        }
    }
}

