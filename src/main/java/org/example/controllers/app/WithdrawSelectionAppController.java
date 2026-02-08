package org.example.controllers.app;

import javafx.application.Platform;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WithdrawSelectionAppController {
    private final Logger logger = Logger.getLogger(WithdrawSelectionAppController.class.getName());
    private BigDecimal available = BigDecimal.ZERO;
    private Integer userId;

    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "seller-ui-worker");
        t.setDaemon(true);
        return t;
    });

    public BigDecimal loadBalance() {
        try {
            userId = Session.getUserId();
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

    public List<CardViewModel> loadSavedCards() {
        try {
            if (userId == null) return List.of();
            return CardsService.loadSavedCards(userId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore caricamento carte", e);
            return List.of();
        }
    }

    public void addInlineCardAsync(String holder, String number, String expiry, String type,
                                   Runnable onSuccess, Consumer<String> onError) {
        if (userId == null) {
            onError.accept("Utente non loggato");
            return;
        }
        runAsync(() -> {
            InlineCardData data = new InlineCardData(holder, number, expiry, type);
            AddCardResult result = CardsService.addInlineCard(userId, data);
            if (!result.ok()) throw new IllegalArgumentException(result.message());
        }, onSuccess, toStringConsumer(onError));
    }

    public void confirmWithdrawAsync(Card card, String cvv, BigDecimal amount,
                                     Runnable onSuccess, Consumer<String> onError) {
        if (userId == null || amount.compareTo(available) > 0) {
            onError.accept("Saldo insufficiente o utente non loggato");
            return;
        }
        runAsync(() -> {
            confirmWithdraw(amount, card, cvv);
            return null;
        }, v -> onSuccess.run(), toStringConsumer(onError));
    }

    // Aggiungi questo metodo
    private Consumer<Throwable> toStringConsumer(Consumer<String> errorHandler) {
        return throwable -> errorHandler.accept(
                throwable.getMessage() != null ? throwable.getMessage() : "Errore sconosciuto"
        );
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
        try {
            this.available = ShopDAO.getBalance(userId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore preload balance", e);
            this.available = BigDecimal.ZERO;
        }
    }

    public void confirmWithdraw(BigDecimal amount, Card card, String cvv) {

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

    public <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        EXEC.submit(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Throwable ex) {  // Throwable per catturare tutto
                logger.log(Level.SEVERE, "Errore operazione async", ex);
                Platform.runLater(() -> onError.accept(ex));  // Passa direttamente ex
            }
        });
    }

    public void runAsync(Runnable task, Runnable onSuccess, Consumer<Throwable> onError) {
        EXEC.submit(() -> {
            try {
                task.run();
                Platform.runLater(onSuccess);
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, "Errore async", ex);
                Platform.runLater(() -> onError.accept(ex));
            }
        });
    }

}
