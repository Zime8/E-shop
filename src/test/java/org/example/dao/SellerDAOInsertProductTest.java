package org.example.dao;

import org.example.database.DatabaseConnection;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SellerDAOInsertProductTest {

    private static final int VENDOR_USER_ID = 1;   // esiste nel DB originale
    private static final int PRODUCT_ID     = 3;   // Puma Future Z 1.4 nel DB originale
    private String createdSize = null;             // per cleanup
    private int shopId;

    @BeforeAll
    void sanity() throws Exception {
        try (Connection ignored = DatabaseConnection.getInstance()) { /* ok */ }
        var shop = SellerDAO.findShopForUser(VENDOR_USER_ID);
        assertNotNull(shop, "Nessuno shop associato al venditore");
        shopId = shop.shopId();
    }

    @AfterEach
    void cleanup() throws Exception {
        if (createdSize == null) return;
        try (var c = DatabaseConnection.getInstance();
             var ps = c.prepareStatement(
                     "DELETE FROM product_availability WHERE id_shop=? AND product_id=? AND size=?")) {
            ps.setInt(1, shopId);
            ps.setInt(2, PRODUCT_ID);
            ps.setString(3, createdSize);
            ps.executeUpdate();
        } finally {
            createdSize = null;
        }
    }

    @Test
    @DisplayName("Il venditore inserisce una NUOVA variante prodotto nel proprio shop (size libera)")
    void insertNewVariantIntoOwnShop() throws Exception {
        // 1) Trova una taglia NON presente per (shopId, PRODUCT_ID)
        String size = findFreeSize(shopId);
        assertNotNull(size, "Non trovata una taglia libera per il test");
        createdSize = size; // per cleanup

        // 2) Primo upsert → deve creare la riga con prezzo e qty specificati
        BigDecimal firstPrice = new BigDecimal("119.99");
        int firstQty = 5;
        SellerDAO.upsertCatalogRow(shopId, PRODUCT_ID, size, firstPrice, firstQty);

        SellerDAO.CatalogRow row1 = findCatalogRow(shopId, size);
        assertNotNull(row1, "La variante inserita non è stata trovata nel catalogo");
        assertEquals(firstPrice, row1.price(), "Prezzo iniziale non corrisponde");
        assertEquals(firstQty, row1.quantity(), "Quantità iniziale non corrisponde");

        // 3) Secondo upsert → nel tuo SP originale la quantità si somma e il prezzo NON cambia
        BigDecimal newPrice = new BigDecimal("100.00");
        int addQty = 2;
        SellerDAO.upsertCatalogRow(shopId, PRODUCT_ID, size, newPrice, addQty);

        SellerDAO.CatalogRow row2 = findCatalogRow(shopId, size);
        assertNotNull(row2, "Variante non trovata dopo secondo upsert");
        assertEquals(firstPrice, row2.price(), "Il prezzo NON dovrebbe cambiare con l’upsert");
        assertEquals(firstQty + addQty, row2.quantity(), "La quantità non è stata sommata correttamente");
    }

    // --- helpers -------------------------------------------------------------

    /** Restituisce la prima taglia “plausibile” non presente per (shopId, productId). */
    private String findFreeSize(int shopId) throws Exception {
        Set<String> used = new HashSet<>();
        try (var c = DatabaseConnection.getInstance();
             var ps = c.prepareStatement("""
                 SELECT size FROM product_availability
                 WHERE id_shop=? AND product_id=?""")) {
            ps.setInt(1, shopId);
            ps.setInt(2, SellerDAOInsertProductTest.PRODUCT_ID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) used.add(rs.getString(1));
            }
        }
        // elenco di taglie comuni per calzature; aggiungiamo fallback
        List<String> candidateSizes = new ArrayList<>(List.of(
                "38","39","40","41","42","43","44","45","46","47","48"
        ));
        // se proprio tutte occupate, genera un'etichetta di test
        for (String s : candidateSizes) {
            if (!used.contains(s)) return s;
        }
        // fallback assoluto
        String fallback = "ZZ" + System.currentTimeMillis()%1000;
        return used.contains(fallback) ? null : fallback;
    }

    private SellerDAO.CatalogRow findCatalogRow(int shopId, String size) throws Exception {
        var catalog = SellerDAO.listCatalog(shopId, null);
        return catalog.stream()
                .filter(r -> r.productId() == SellerDAOInsertProductTest.PRODUCT_ID && size.equals(r.size()))
                .findFirst()
                .orElse(null);
    }
}
