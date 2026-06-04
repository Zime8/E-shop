package org.example.control;

import javafx.application.Platform;
import org.example.control.services.PaymentSelectionService;
import org.example.dao.ShopRepository;
import org.example.models.dto.Card;
import org.example.models.dto.InlineCardData;
import org.example.control.services.CardsService.AddCardResult;
import org.example.util.CardValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Withdraw {
    private final ShopRepository shopRepository;
    private final PaymentSelectionService paymentSelectionService;
    private final Logger logger = Logger.getLogger(Withdraw.class.getName());

    public Withdraw(ShopRepository shopRepository, PaymentSelectionService paymentSelectionService) {
        this.shopRepository = shopRepository;
        this.paymentSelectionService = paymentSelectionService;
    }

    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "seller-ui-worker");
        t.setDaemon(true);
        return t;
    });

    public BigDecimal loadBalance(int userId) {
        if(userId <= 0){
            throw new IllegalArgumentException("User id non valido");
        }
        try {
            return shopRepository.getBalance(userId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento balance", e);
            return BigDecimal.ZERO;
        }
    }

    public List<Card> loadSavedCards(int userId) {
        if(userId <= 0){
            throw new IllegalArgumentException("User id non valido");
        }
        try {
            return paymentSelectionService.loadSavedCards(userId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento carte", e);
            return List.of();
        }
    }

    public void addInlineCardAsync(int userId, String holder, String number, String expiry, String type,
                                   Runnable onSuccess, Consumer<String> onError) {
        runAsync(() -> {
            InlineCardData data = new InlineCardData(holder, number, expiry, type);
            AddCardResult result = paymentSelectionService.addInlineCard(userId, data);
            if (!result.ok()) throw new IllegalArgumentException(result.message());
        }, onSuccess, toStringConsumer(onError));
    }

    public void confirmWithdrawAsync(int userId, Card card, String cvv, BigDecimal amount,
                                     Runnable onSuccess, Consumer<String> onError) {
        runAsync(() -> {
            try {
                Thread.sleep(900);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AppControllerException("Operazione interrotta", e);
            }

            confirmWithdraw(userId, amount, card, cvv);
            return null;
        }, v -> onSuccess.run(), toStringConsumer(onError));
    }

    private Consumer<Throwable> toStringConsumer(Consumer<String> errorHandler) {
        return throwable -> errorHandler.accept(
                throwable.getMessage() != null ? throwable.getMessage() : "Errore sconosciuto"
        );
    }

    public void confirmWithdraw(int userId, BigDecimal amount, Card card, String cvv) {

        if (userId <= 0 || amount == null || card == null || cvv == null) {
            throw new IllegalArgumentException("Dati prelievo invalidi");
        }

        if (amount.signum() <= 0){
            throw new IllegalArgumentException("Importo non valido");
        }

        if (!CardValidator.isValidCvv(cvv)) {
            throw new IllegalArgumentException("CVV non valido");
        }

        try {
            BigDecimal available = shopRepository.getBalance(userId);
            if (amount.compareTo(available) > 0) {
                throw new IllegalArgumentException("Saldo insufficiente");
            }

            // Esegui prelievo
            shopRepository.requestWithdraw(userId, amount);
            logger.log(Level.INFO, "Prelievo effettuato: {0}€ per user {1}",
                    new Object[]{amount, userId});
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore conferma prelievo", e);
            throw new AppControllerException("Errore durante il prelievo", e);
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
