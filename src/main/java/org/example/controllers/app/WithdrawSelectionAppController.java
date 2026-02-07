package org.example.controllers.app;

import org.example.control.interfaces.WithdrawSelectionControl;
import org.example.dao.ShopDAO;
import org.example.models.Card;
import org.example.models.CardViewModel;
import org.example.models.InlineCardData;
import org.example.control.services.CardsService;
import org.example.control.services.CardsService.AddCardResult;
import org.example.util.CardValidator;
import org.example.util.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WithdrawSelectionAppController implements WithdrawSelectionControl {
    private final Logger logger = Logger.getLogger(WithdrawSelectionAppController.class.getName());
    private BigDecimal available = BigDecimal.ZERO;

    @Override
    public BigDecimal loadBalance() {
        try {
            Integer userId = Session.getUserId();
            if (userId == null) {
                logger.warning("User ID null");
                return BigDecimal.ZERO;
            }
            available = ShopDAO.getBalance(userId);
            return available;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento balance", e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public List<CardViewModel> loadSavedCards() {
        try {
            Integer userId = Session.getUserId();
            if (userId == null) return List.of();
            return CardsService.loadSavedCards(userId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore caricamento carte", e);
            return List.of();
        }
    }

    @Override
    public void addInlineCard(String holder, String number, String expiry, String type) {
        Integer userId = Session.getUserId();
        if (userId == null) {
            logger.warning("User ID null per add card");
            return;
        }

        InlineCardData data = new InlineCardData(holder, number, expiry, type);
        AddCardResult result = CardsService.addInlineCard(userId, data);

        if (!result.ok()) {
            logger.log(Level.WARNING, "Add inline card failed: {0}", result.message());
        }
    }

    @Override
    public void confirmWithdraw(BigDecimal amount, Card card, String cvv) {
        Integer userId = Session.getUserId();
        if (userId == null || amount == null || card == null || cvv == null) {
            throw new IllegalArgumentException("Dati prelievo invalidi");
        }

        if (!CardValidator.isValidCvv(cvv)) {
            throw new IllegalArgumentException("CVV non valido");
        }

        if (amount.compareTo(available) > 0) {
            throw new IllegalArgumentException("Saldo insufficiente");
        }

        try {
            // Esegui prelievo
            ShopDAO.requestWithdraw(userId, amount);
            logger.log(Level.INFO, "Prelievo effettuato: {0}€ per user {1}",
                    new Object[]{amount, userId});
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore conferma prelievo", e);
        }
    }
}
