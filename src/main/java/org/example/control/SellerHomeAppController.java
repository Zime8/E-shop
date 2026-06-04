package org.example.control;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import org.example.control.session.UserContext;
import org.example.dao.ShopRepository;
import org.example.config.AppExecutors;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SellerHomeAppController {

    private Integer currentShopId;
    private final ShopRepository shopRepository;
    private final UserContext userContext;
    private final ExecutorService executor;

    private static final List<String> ORDER_STATES =
            List.of("in elaborazione", "spedito", "consegnato", "annullato");

    private static final Logger logger =
            Logger.getLogger(SellerHomeAppController.class.getName());

    public SellerHomeAppController(ShopRepository shopRepository, UserContext userContext) {
        this(shopRepository, userContext, AppExecutors.IO);
    }

    public SellerHomeAppController(ShopRepository shopRepository,
                                   UserContext userContext,
                                   ExecutorService executor) {
        this.shopRepository = shopRepository;
        this.userContext = userContext;
        this.executor = executor;
    }

    public Integer getCurrentUserId() {
        return userContext.getCurrentUserId();
    }

    public Integer getCurrentShopId() {
        return currentShopId;
    }

    public void ensureUserLoggedIn(Runnable onLoggedIn, Runnable onNotLoggedIn) {
        if (userContext.isLoggedIn()) {
            onLoggedIn.run();
        } else {
            onNotLoggedIn.run();
        }
    }

    public void populateOrderStates(ComboBox<String> orderStateFilter,
                                    ComboBox<String> orderStateCombo) {
        orderStateFilter.getItems().setAll(ORDER_STATES);
        orderStateCombo.getItems().setAll(ORDER_STATES);
    }

    public void loadSellerShop(Consumer<String> onShopName, Consumer<String> onError) {
        Integer userId = userContext.getCurrentUserId();
        if (userId == null) {
            onError.accept("Utente non loggato.");
            return;
        }

        runAsync(() -> shopRepository.findShopForUser(userId.longValue()),
                optShop -> {
                    if (optShop.isEmpty()) {
                        onError.accept("Nessun negozio associato.");
                        return;
                    }
                    var shop = optShop.get();
                    this.currentShopId = Math.toIntExact(shop.shopId());
                    onShopName.accept(shop.shopName());
                },
                e -> onError.accept("Errore DB: " + e.getMessage())
        );
    }

    public void refreshBalance(Consumer<BigDecimal> onBalance, Consumer<String> onError) {
        Integer userId = userContext.getCurrentUserId();
        if (userId == null) {
            onError.accept("Non loggato - userId null");
            return;
        }

        runAsync(() -> shopRepository.getBalance(userId),
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
        Integer userId = userContext.getCurrentUserId();
        if (userId == null) {
            return false;
        }

        BigDecimal saldo = shopRepository.getBalance(userId);
        return saldo != null && saldo.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean logout() {
        userContext.logout();
        return true;
    }

    public String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public BigDecimal nonNull(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    public <T> void runAsync(Callable<T> task,
                             Consumer<T> onSuccess,
                             Consumer<Exception> onError) {
        executor.submit(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore operazione async", ex);
                Platform.runLater(() ->
                        onError.accept(ex instanceof SQLException ? ex : new Exception(ex)));
            }
        });
    }
}