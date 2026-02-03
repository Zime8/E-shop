package org.example.controllers.app;

import javafx.application.Platform;
import javafx.scene.control.*;
import org.example.dao.SellerDAO;
import org.example.dao.ShopDAO;
import org.example.models.CatalogForm;
import org.example.util.Session;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
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
    private static final NumberFormat CURR_IT = NumberFormat.getCurrencyInstance(Locale.ITALY);

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

    public void refreshBalance(Label balanceLabel, Button withdrawButton, Consumer<String> onError)  {
        try{
            Integer userId = Session.getUserId();
            if(userId == null){
                updateBalanceUI(balanceLabel, withdrawButton, null);
                return;
            }
            BigDecimal bal = ShopDAO.getBalance(userId);
            updateBalanceUI(balanceLabel, withdrawButton, bal);
        }catch (SQLException e) {
            logger.info( "Aggiornamento saldo fallito");
            updateBalanceUI(balanceLabel, withdrawButton, null);
            onError.accept("Errore nel refresh balance");
        }
    }

    private void updateBalanceUI(Label label, Button button, BigDecimal balance){
        if(balance == null || balance.compareTo(BigDecimal.ZERO) <= 0){
            label.setText("-");
            button.setDisable(true);
        } else {
            label.setText(CURR_IT.format(balance));
            button.setDisable(false);
        }
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

    public void reloadCatalog(Consumer<List<SellerDAO.CatalogRow>> onSuccess, Consumer<String> onError){
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

    public void listOrderAsync(String stateFilter, Consumer<List<SellerDAO.ShopOrderSummary>> onSuccess,
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
        Session.clear();
        return true;
    }

    public void loadOrderLines(int orderId, Consumer<List<SellerDAO.ShopOrderLine>> onSuccess, Consumer<String> onError) {
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
                                       Consumer<List<SellerDAO.ProductOption>> onSuccess,
                                       Consumer<String> onError) {
        runAsync(
                () -> SellerDAO.listProductOptionsByNameLike(query, limit),
                onSuccess,
                e -> onError.accept("Errore nel caricamento prodotti: " + e.getMessage())
        );
    }

    public void loadAllProducts(ComboBox<SellerDAO.ProductOption> combo, Consumer<String> onError) {
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

    public boolean matchesAllTokens(SellerDAO.ProductOption opt, String[] tokens) {
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
