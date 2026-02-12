package org.example.controllers.app;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import org.example.dao.SellerDAO;
import org.example.dao.ShopDAO;
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

public class SellerHomeAppController {

    private Integer currentShopId;
    private static final List<String> ORDER_STATES = List.of("in elaborazione", "spedito", "consegnato", "annullato");

    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "seller-ui-worker");
        t.setDaemon(true);
        return t;
    });
    private static final Logger logger = Logger.getLogger(SellerHomeAppController.class.getName());

    // --- Stato utente/logged info ---
    public Integer getCurrentUserId() {
        return Session.getUserId();
    }

    public Integer getCurrentShopId(){
        return currentShopId;
    }

    public void ensureUserLoggedIn(Runnable onLoggedIn, Runnable onNotLoggedIn) {
        if (Session.getUserId() != null) {
            onLoggedIn.run();
        } else {
            onNotLoggedIn.run();
        }
    }

    public void populateOrderStates(ComboBox<String> orderStateFilter, ComboBox<String> orderStateCombo) {
        orderStateFilter.getItems().setAll(ORDER_STATES);
        orderStateCombo.getItems().setAll(ORDER_STATES);
    }

    public void loadSellerShop(Consumer<String> onShopName, Consumer<String> onError) {
        try {
            var shop = SellerDAO.findShopForUser(Session.getUserId());
            if (shop == null) {
                onError.accept("Nessun negozio associato.");
                return;
            }
            this.currentShopId = shop.shopId();
            Platform.runLater(() -> onShopName.accept(shop.shopName()));
        } catch (SQLException e) {
            onError.accept("Errore DB: " + e.getMessage());
        }
    }

    // --- Saldo / prelievo ---
    public void refreshBalance(Consumer<BigDecimal> onBalance, Consumer<String> onError) {
        Integer userId = Session.getUserId();
        if (userId == null) {
            onError.accept("Non loggato - userId null");
            return;
        }

        runAsync(() -> ShopDAO.getBalance(userId),
                balance -> onBalance.accept(nonNull(balance)),
                e -> onError.accept("Errore saldo: " + e.getMessage())
        );
    }

    public void withdrawRequest(Runnable onHasBalance,
                                Runnable onNoBalance,
                                Runnable onNotLogged) {
        ensureUserLoggedIn(
                () -> {
                    if (hasAvailableBalance()) {
                        onHasBalance.run();
                    } else {
                        onNoBalance.run();
                    }
                },
                onNotLogged
        );
    }

    public boolean hasAvailableBalance() {
        Integer userId = Session.getUserId();
        if (userId == null) return false;

        try {
            BigDecimal saldo = ShopDAO.getBalance(userId);
            return saldo != null && saldo.compareTo(BigDecimal.ZERO) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // --- Logout ---
    public boolean logout() {
        Session.logout();
        return true;
    }

    // --- Utilità su string/BigDecimal ---
    public String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public BigDecimal nonNull(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    // --- Async helper (potresti eventualmente spostarlo in una classe comune) ---
    public <T> void runAsync(Callable<T> task,
                             Consumer<T> onSuccess,
                             Consumer<Exception> onError) {
        EXEC.submit(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore operazione async", ex);
                Platform.runLater(() -> onError.accept(ex instanceof SQLException ? ex : new Exception(ex)));
            }
        });
    }

}
