package org.example.controllers.app;

import javafx.application.Platform;
import javafx.scene.control.*;
import org.example.dao.SellerDAO;
import org.example.dao.ShopDAO;
import org.example.models.*;
import org.example.util.Session;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SellerHomeAppController {

    private Integer currentShopId;

    private static final List<String> ORDER_STATES = List.of("in elaborazione", "spedito", "consegnato", "annullato");

    // Esecutore async
    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "seller-ui-worker");
        t.setDaemon(true);
        return t;
    });
    private static final Logger logger = Logger.getLogger(SellerHomeAppController.class.getName());

    public Integer getCurrentUserId(){
        return Session.getUserId();
    }

    public void ensureUserLoggedIn(Runnable onLoggedIn, Runnable onNotLoggedIn) {
        if (Session.getUserId() != null) {
            onLoggedIn.run();
        } else {
            onNotLoggedIn.run();
        }
    }

    public void loadSellerShop(Consumer<String> onShopName, Consumer<String> onError) {
        try {
            var shop = SellerDAO.findShopForUser(Session.getUserId());
            if (shop == null) {
                onError.accept("Nessun negozio associato.");
                return;
            }
            currentShopId = shop.shopId();
            Platform.runLater(() -> onShopName.accept(shop.shopName()));
        } catch (SQLException e) {
            onError.accept("Errore DB: " + e.getMessage());
        }
    }

    public void populateOrderStates(ComboBox<String> orderStateFilter, ComboBox<String>orderStateCombo) {
        orderStateFilter.getItems().setAll(ORDER_STATES);
        orderStateCombo.getItems().setAll(ORDER_STATES);
    }

    public void refreshBalance(Consumer<BigDecimal> onBalance, Consumer<String> onError) {
        Integer userId = Session.getUserId();

        if (userId == null) {
            onError.accept("Non loggato - userId null");
            return;
        }

        runAsync(() -> ShopDAO.getBalance(userId),
                onBalance,
                e -> onError.accept("Errore saldo: " + e.getMessage())
        );
    }


    public void withdrawRequest(Runnable onHasBalance, Runnable onNoBalance, Runnable onNotLogged) {
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

    public boolean hasAvailableBalance(){
        Integer userId = Session.getUserId();
        if (userId == null) return false;

        try {
            BigDecimal saldo = ShopDAO.getBalance(userId);
            return saldo != null && saldo.compareTo(BigDecimal.ZERO) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public void prefillProductName(int productId, Consumer<String> onName, Runnable onNotFound) {
        runAsync(
                () -> SellerDAO.listAllProductOptions().stream()
                        .filter(o -> o.productId() == productId)
                        .findFirst()
                        .map(ProductOption::name),
                optName -> optName.ifPresentOrElse(onName, onNotFound),
                e -> { onNotFound.run(); logger.warning("Product name load failed: " + e); }
        );
    }

    public void searchProductOptions(String firstToken, int limit, String[] allTokens,
                                     Consumer<List<ProductOption>> onResults, Consumer<String> onError) {
        runAsync(
                () -> {
                    List<ProductOption> all = SellerDAO.listProductOptionsByNameLike(firstToken, limit);
                    return all.stream()
                            .filter(o -> matchesAllTokens(o, allTokens))  // Riutilizza tuo metodo
                            .toList();
                },
                onResults,
                e -> onError.accept("Errore ricerca: " + e.getMessage())
        );
    }

    public void reloadCatalog(Consumer<List<CatalogRow>> onSuccess, Consumer<String> onError){
        runAsync(
                () -> SellerDAO.listCatalog(currentShopId, null),
                onSuccess,
                e -> onError.accept("Errore nel caricamento catalogo: " + e.getMessage())
        );
    }

    public void addProductAsync(CatalogForm data, Runnable onSuccess, Consumer<String> onError) {
        runAsync(() -> {
                    SellerDAO.upsertCatalogRow(currentShopId, data.productId(), data.size(),
                            data.price(), data.quantity());
                    return null;
                }, onSuccess,
                e -> Platform.runLater(() -> onError.accept("Errore durante l'aggiunta: " + e.getMessage()))
        );
    }

    public void editProductAsync(CatalogForm data, Runnable onSuccess, Consumer<String> onError) {
        runAsync(
                () -> {
                    SellerDAO.updateCatalogRow(currentShopId,
                        data.productId(), data.size(), data.price(), data.quantity());
                    return null;
                },
                result -> Platform.runLater(onSuccess),
                e -> Platform.runLater(() -> onError.accept("Errore durante la modifica: " + e.getMessage()))
        );
    }

    public void deleteProductAsync(int productId, String size, Runnable onSuccess, Consumer<String> onError) {
        runAsync(
                () -> {
                    SellerDAO.deleteCatalogRow(currentShopId, productId, size);
                    return null;
                },
                result -> Platform.runLater(onSuccess),
                e -> Platform.runLater(() -> onError.accept("Errore durante l'eliminazione: " + e.getMessage()))
        );
    }

    public void listOrderAsync(String stateFilter, Consumer<List<ShopOrderSummary>> onSuccess,
                               Consumer<String> onError) {
        runAsync(
                () -> SellerDAO.listShopOrders(currentShopId, stateFilter),
                onSuccess,
                e -> onError.accept("Errore caricamento ordini: " + e.getMessage())
        );
    }

    public void updateOrderStatusAsync(int orderId, String newState, Runnable onSuccess,
                                       Consumer<String> onError) {
        runAsync(
                () -> {
                    SellerDAO.updateOrderState(orderId, newState);
                    return null;
                },
                result -> Platform.runLater(onSuccess),
                e -> onError.accept("Errore durante l'aggiornamento dello stato: " + e.getMessage())
        );
    }

    public boolean logout(){
        Session.logout();
        return true;
    }

    public void loadOrderLines(int orderId, Consumer<List<ShopOrderLine>> onSuccess, Consumer<String> onError) {
        runAsync(
                () -> SellerDAO.listShopOrderLines(currentShopId, orderId),
                onSuccess,
                e -> onError.accept("Errore nel caricamento dettagli ordine: " + e.getMessage())
        );
    }

    public String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public BigDecimal nonNull(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    public boolean isValidCatalogForm(CatalogForm form) {
        return form.price().compareTo(BigDecimal.ZERO) > 0 && form.quantity() > 0;
    }

    public void loadProductOptionAsync(String query, int limit,
                                       Consumer<List<ProductOption>> onSuccess,
                                       Consumer<String> onError) {
        runAsync(
                () -> SellerDAO.listProductOptionsByNameLike(query, limit),
                onSuccess,
                e -> onError.accept("Errore nel caricamento prodotti: " + e.getMessage())
        );
    }

    public void loadAllProducts(ComboBox<ProductOption> combo, Consumer<String> onError) {
        loadProductOptionAsync("", 100,
                opts -> {
                    combo.getItems().setAll(opts);
                    if(combo.isShowing()) combo.show();
                },
                onError
        );
    }

    public String extractNameForSearch(String s) {
        if (s == null) return "";
        String t = s.trim();
        int dot = t.indexOf('·');
        if (dot >= 0) t = t.substring(0, dot);
        int par = t.indexOf('(');
        if (par >= 0) t = t.substring(0, par);
        return normalizeQuery(t);
    }

    public String normalizeQuery(String s) {
        return (s == null ? "" : s)
                .toLowerCase(Locale.ITALIAN)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public boolean matchesAllTokens(ProductOption opt, String[] tokens) {
        String name = normalizeQuery(opt.name());
        for (String t : tokens) {
            if (!t.isBlank() && !name.contains(t)) return false;
        }
        return true;
    }

    public <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
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

    private void runAsync(Callable<Void> task, Runnable onSuccess, Consumer<Exception> onError) {
        runAsync(task, v -> onSuccess.run(), onError);
    }
}
