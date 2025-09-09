package org.example.dao;

import org.example.database.DatabaseConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SellerDAOInsertProductTest {

    // Dati di test: usiamo il venditore con id=1 (Cisalfa),
    // il prodotto con id=3 (Puma Future Z 1.4),
    // e una taglia "41" (che nel seed non c’è, così possiamo testare l’inserimento).
    private static final int VENDOR_USER_ID = 1;
    private static final int PRODUCT_ID     = 3;
    private static final String SIZE        = "41";

    @Test
    @DisplayName("Il venditore inserisce una nuova variante prodotto nel proprio shop")
    void insertNewVariantIntoOwnShop() throws Exception {
        // --- 1) Verifica che il database sia raggiungibile
        try (Connection ignored = DatabaseConnection.getInstance()) {
            // se non lancia eccezioni, la connessione è ok
        }

        // --- 2) Recupera lo shop associato al venditore
        SellerDAO.SellerShop shop = SellerDAO.findShopForUser(VENDOR_USER_ID);
        assertNotNull(shop, "Nessuno shop associato al venditore");
        int shopId = shop.shopId(); // ID del negozio di Cisalfa

        // --- 3) Primo inserimento di una nuova variante (upsert)
        BigDecimal firstPrice = new BigDecimal("119.99");
        int firstQty = 5;

        // Inserisce nel catalogo il prodotto 3, taglia 41, prezzo 119.99, quantità 5
        SellerDAO.upsertCatalogRow(shopId, PRODUCT_ID, SIZE, firstPrice, firstQty);

        // --- 4) Verifica dopo il primo upsert
        SellerDAO.CatalogRow row1 = findCatalogRow(shopId, PRODUCT_ID, SIZE);
        assertNotNull(row1, "La variante inserita non è stata trovata nel catalogo");
        assertEquals(firstPrice, row1.price(), "Prezzo iniziale non corrisponde");
        assertEquals(firstQty, row1.quantity(), "Quantità iniziale non corrisponde");

        // --- 5) Secondo upsert sulla stessa variante
        BigDecimal newPrice = new BigDecimal("100.00");
        int addQty = 2;

        // Inserisce di nuovo la stessa variante con prezzo 100.00 e quantità aggiuntiva 2
        SellerDAO.upsertCatalogRow(shopId, PRODUCT_ID, SIZE, newPrice, addQty);

        // --- 6) Verifica dopo il secondo upsert
        SellerDAO.CatalogRow row2 = findCatalogRow(shopId, PRODUCT_ID, SIZE);
        assertNotNull(row2);

        // Il prezzo rimane quello iniziale (la stored procedure non aggiorna il prezzo)
        assertEquals(firstPrice, row2.price(), "Il prezzo NON dovrebbe cambiare con l’upsert");

        // La quantità è stata sommata (5 iniziali + 2 aggiunti = 7)
        assertEquals(firstQty + addQty, row2.quantity(), "La quantità non è stata sommata correttamente");
    }

    // --- Helper per cercare la riga del catalogo con shopId, productId e size
    private SellerDAO.CatalogRow findCatalogRow(int shopId, int productId, String size) throws Exception {
        List<SellerDAO.CatalogRow> catalog = SellerDAO.listCatalog(shopId, null);
        return catalog.stream()
                .filter(r -> r.productId() == productId && size.equals(r.size()))
                .findFirst()
                .orElse(null);
    }
}
