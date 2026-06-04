package org.example.control;

import javafx.application.Platform;
import org.example.dao.db.DbSellerDAO;
import org.example.models.dto.ShopOrderSummary;
import org.example.models.dto.ShopOrderLine;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SellerOrdersController {

    private Integer currentShopId;

    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "seller-ui-worker");
        t.setDaemon(true);
        return t;
    });
    private static final Logger logger = Logger.getLogger(SellerOrdersController.class.getName());

    public void setCurrentShopId(Integer shopId) {
        this.currentShopId = shopId;
    }

    public void listOrderAsync(String stateFilter,
                               Consumer<List<ShopOrderSummary>> onSuccess,
                               Consumer<String> onError) {

        runAsync(
                () -> DbSellerDAO.listShopOrders(currentShopId, stateFilter),
                onSuccess,
                e -> onError.accept("Errore caricamento ordini: " + e.getMessage())
        );
    }

    public void updateOrderStatusAsync(int orderId, String newState,
                                       Runnable onSuccess, Consumer<String> onError) {
        if (currentShopId == null) {
            onError.accept("Nessun negozio selezionato");
            return;
        }

        runAsync(
                () -> {
                    DbSellerDAO.updateOrderState(orderId, newState);
                    return null;
                },
                result -> Platform.runLater(onSuccess),
                e -> onError.accept("Errore durante l'aggiornamento dello stato: " + e.getMessage())
        );
    }

    public void loadOrderLines(int orderId,
                               Consumer<List<ShopOrderLine>> onSuccess,
                               Consumer<String> onError) {
        if (currentShopId == null) {
            onError.accept("Nessun negozio selezionato");
            return;
        }

        runAsync(
                () -> DbSellerDAO.listShopOrderLines(currentShopId, orderId),
                onSuccess,
                e -> onError.accept("Errore nel caricamento dettagli ordine: " + e.getMessage())
        );
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

}
