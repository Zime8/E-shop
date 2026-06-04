package org.example.control;

import org.example.control.services.SavedCardsService;
import org.example.control.session.UserContext;
import org.example.dao.UserRepository;
import org.example.dao.WishlistRepository;
import org.example.models.dto.Card;
import org.example.models.dto.ProfileDataDto;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

public class ModifyProfile {

    private static final String PSW = "******";
    private static final Logger logger = Logger.getLogger(ModifyProfile.class.getName());
    private final SavedCardsService savedCardsService;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;
    private final UserContext userContext;

    public ModifyProfile(UserRepository userRepository,
                         WishlistRepository wishlistRepository,
                         SavedCardsService savedCardsService,
                         UserContext userContext) {
        this.userRepository = userRepository;
        this.wishlistRepository = wishlistRepository;
        this.savedCardsService = savedCardsService;
        this.userContext = userContext;
    }

    public void loadUserData(Consumer<ProfileDataDto> onDataLoaded) {
        String username = userContext.getCurrentUsername();
        if (username == null || username.isBlank()) {
            onDataLoaded.accept(emptyProfileData());
            return;
        }

        try {
            var u = userRepository.findByUsername(username);
            if (u == null) {
                onDataLoaded.accept(emptyProfileData());
                return;
            }

            onDataLoaded.accept(new ProfileDataDto(
                    u.getUsername(),
                    PSW,
                    u.getEmail(),
                    u.getPhone()
            ));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei dati utente", e);
            onDataLoaded.accept(emptyProfileData());
        }
    }

    public String getCurrentUsername() {
        return userContext.getCurrentUsername();
    }

    public void updateProfile(String currentUsername, String newUsername, String newEmail,
                              String newPhone, String newPwd,
                              Consumer<Boolean> onSuccess, Consumer<String> onError) {
        try {
            if (newPwd != null && !newPwd.isBlank()) {
                userRepository.updateProfileWithPassword(currentUsername, newUsername, newEmail, newPhone, newPwd);
            } else {
                userRepository.updateProfile(currentUsername, newUsername, newEmail, newPhone);
            }

            if (!Objects.equals(currentUsername, newUsername)) {
                wishlistRepository.renameWishlistOwner(currentUsername, newUsername);
            }

            // Aggiorna sessione
            userContext.setUsername(newUsername);

            onSuccess.accept(true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante l'aggiornamento dei dati utente", e);
            onError.accept(
                    e.getMessage() != null && !e.getMessage().isBlank()
                            ? e.getMessage()
                            : "Errore durante l'aggiornamento del profilo"
            );
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

    private ProfileDataDto emptyProfileData() {
        return new ProfileDataDto("", PSW, "", "");
    }
}

