package org.example.control;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import org.example.dao.db.DbSellerDAO;
import org.example.models.dto.CatalogRow;
import org.example.models.dto.CatalogForm;
import org.example.models.dto.ProductOption;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SellerProductsController {

    private Integer currentShopId;

    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "seller-ui-worker");
        t.setDaemon(true);
        return t;
    });
    private static final Logger logger = Logger.getLogger(SellerProductsController.class.getName());

    public void setCurrentShopId(Integer shopId) {
        this.currentShopId = shopId;
    }

    public void reloadCatalog(Consumer<List<CatalogRow>> onSuccess,
                              Consumer<String> onError) {
        if (currentShopId == null) {
            logger.warning("reloadCatalog: shopId NULL");
            onError.accept("Shop non selezionato");
            return;
        }

        runAsync(
                () -> DbSellerDAO.listCatalog(currentShopId, null),
                onSuccess,
                e -> onError.accept("Errore nel caricamento catalogo: " + e.getMessage())
        );
    }

    public void addProductAsync(CatalogForm data,
                                Runnable onSuccess,
                                Consumer<String> onError) {
        if (currentShopId == null) {
            onError.accept("Nessun negozio selezionato");
            return;
        }

        runAsync(() -> {
                    DbSellerDAO.upsertCatalogRow(currentShopId, data.productId(), data.size(),
                            data.price(), data.quantity());
                    return null;
                },
                onSuccess,
                e -> Platform.runLater(() -> onError.accept("Errore durante l'aggiunta: " + e.getMessage()))
        );
    }

    public void editProductAsync(CatalogForm data,
                                 Runnable onSuccess,
                                 Consumer<String> onError) {
        if (currentShopId == null) {
            onError.accept("Nessun negozio selezionato");
            return;
        }

        runAsync(
                () -> {
                    DbSellerDAO.updateCatalogRow(currentShopId,
                            data.productId(), data.size(), data.price(), data.quantity());
                    return null;
                },
                result -> Platform.runLater(onSuccess),
                e -> Platform.runLater(() -> onError.accept("Errore durante la modifica: " + e.getMessage()))
        );
    }

    public void deleteProductAsync(int productId, String size,
                                   Runnable onSuccess,
                                   Consumer<String> onError) {
        if (currentShopId == null) {
            onError.accept("Nessun negozio selezionato");
            return;
        }

        runAsync(
                () -> {
                    DbSellerDAO.deleteCatalogRow(currentShopId, productId, size);
                    return null;
                },
                result -> Platform.runLater(onSuccess),
                e -> Platform.runLater(() -> onError.accept("Errore durante l'eliminazione: " + e.getMessage()))
        );
    }

    public boolean isValidCatalogForm(CatalogForm form) {
        return form.price().compareTo(BigDecimal.ZERO) > 0 && form.quantity() > 0;
    }

    public void loadProductOptionAsync(String query, int limit,
                                       Consumer<List<ProductOption>> onSuccess,
                                       Consumer<String> onError) {
        runAsync(
                () -> DbSellerDAO.listProductOptionsByNameLike(query, limit),
                onSuccess,
                e -> onError.accept("Errore nel caricamento prodotti: " + e.getMessage())
        );
    }

    public void loadAllProducts(ComboBox<ProductOption> combo,
                                Consumer<String> onError) {
        loadProductOptionAsync("", 100,
                opts -> {
                    combo.getItems().setAll(opts);
                    if (combo.isShowing()) combo.show();
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

    public void searchProductOptions(String firstToken, int limit, String[] allTokens,
                                     Consumer<List<ProductOption>> onResults,
                                     Consumer<String> onError) {
        runAsync(
                () -> {
                    List<ProductOption> all = DbSellerDAO.listProductOptionsByNameLike(firstToken, limit);
                    return all.stream()
                            .filter(o -> matchesAllTokens(o, allTokens))
                            .toList();
                },
                onResults,
                e -> onError.accept("Errore ricerca: " + e.getMessage())
        );
    }

    public void prefillProductName(int productId,
                                   Consumer<String> onName,
                                   Runnable onNotFound) {
        runAsync(
                () -> DbSellerDAO.listAllProductOptions().stream()
                        .filter(o -> o.productId() == productId)
                        .findFirst()
                        .map(ProductOption::name),
                optName -> optName.ifPresentOrElse(onName, onNotFound),
                e -> { onNotFound.run(); logger.warning("Product name load failed: " + e); }
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

    private void runAsync(Callable<Void> task, Runnable onSuccess, Consumer<Exception> onError) {
        runAsync(task, v -> onSuccess.run(), onError);
    }
}
