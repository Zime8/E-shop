package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.models.CartItem;
import org.example.util.Session;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderDAOSuccessPaymentTest {

    private Integer createdOrderId = null;

    @BeforeAll
    void useOriginalDb() throws Exception {
        DatabaseConnection.clearOverride();

        Session.setDemo(false);

        // la connessione deve aprirsi
        try (Connection ignored = DatabaseConnection.getInstance()) { /* ok */ }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (createdOrderId == null) return;

        try (Connection c = DatabaseConnection.getInstance()) {
            c.setAutoCommit(false);
            try {
                // Prendo le righe d’ordine per ripristinare stock e calcolare il totale per saldo negozio
                record Line(int shopId, int productId, String size, int qty, double price) {}
                List<Line> lines = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(
                        """
                        SELECT d.id_shop, d.id_product, d.size, d.quantity, d.price
                        FROM details_order d
                        WHERE d.id_order = ?
                        """)) {
                    ps.setInt(1, createdOrderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            lines.add(new Line(
                                    rs.getInt("id_shop"),
                                    rs.getInt("id_product"),
                                    rs.getString("size"),
                                    rs.getInt("quantity"),
                                    rs.getDouble("price")
                            ));
                        }
                    }
                }

                // Ripristino stock e saldo negozio
                double totalPerShop = 0.0;
                Integer shopForBalance = null;

                for (Line ln : lines) {
                    // ripristino stock
                    try (PreparedStatement ps = c.prepareStatement(
                            """
                            UPDATE product_availability
                            SET quantity = quantity + ?
                            WHERE id_shop = ? AND product_id = ? AND size = ?
                            """)) {
                        ps.setInt(1, ln.qty());
                        ps.setInt(2, ln.shopId());
                        ps.setInt(3, ln.productId());
                        ps.setString(4, ln.size());
                        ps.executeUpdate();
                    }

                    // saldo
                    shopForBalance = ln.shopId();
                    totalPerShop += ln.qty() * ln.price();
                }

                if (shopForBalance != null && totalPerShop > 0.0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE shops SET balance = balance - ? WHERE id_shop = ?")) {
                        ps.setDouble(1, totalPerShop);
                        ps.setInt(2, shopForBalance);
                        ps.executeUpdate();
                    }
                }

                // Cancello righe e testata ordine
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM details_order WHERE id_order = ?")) {
                    ps.setInt(1, createdOrderId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM orders_client WHERE id_order = ?")) {
                    ps.setInt(1, createdOrderId);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                createdOrderId = null;
                c.setAutoCommit(true);
            }
        }
    }

    @Test
    @DisplayName("Crea ordine su DB originale e decrementa stock")
    void shouldCreateOrderAndDecrementStock() throws Exception {
        int userId = 2; // cliente esistente nel DB
        int productId;
        int shopId;
        String size;
        double unitPrice;
        int buyQty = 2;

        // prendo una riga reale dal catalogo
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement ps = c.prepareStatement(
                     """
                     SELECT id_shop, product_id, size, price, quantity
                     FROM product_availability
                     WHERE quantity >= 5
                     LIMIT 1
                     """)) {
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Serve almeno una riga di disponibilità con quantity >= 5");
                shopId = rs.getInt("id_shop");
                productId = rs.getInt("product_id");
                size = rs.getString("size");
                unitPrice = rs.getBigDecimal("price").doubleValue();
            }
        }

        int qtyBefore = getStock(shopId, productId, size);

        // carrello con un solo articolo
        CartItem item = new CartItem(productId, shopId, buyQty, unitPrice, null, null, size);
        List<CartItem> items = List.of(item);

        // eseguo l’ordine
        OrderDAO.CreationResult res =
                OrderDAO.placeOrderWithStockDecrement(userId, items, "Via di Test 123, Roma");

        assertNotNull(res, "CreationResult nullo");
        assertEquals(1, res.orderIds().size(), "Con un solo shop deve creare un solo ordine");

        // recupero l’order id creato
        int orderId = res.orderIds().getFirst();
        assertTrue(orderId > 0, "orderId deve essere valido");
        createdOrderId = orderId; // salva per il cleanup

        // verifica che l'ordine esista e appartenga all'utente
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement ps = c.prepareStatement(
                     """
                     SELECT id_user, state_order
                     FROM orders_client
                     WHERE id_order = ?
                     """)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Ordine non trovato in orders_client");
                assertEquals(userId, rs.getInt("id_user"), "L'ordine dovrebbe appartenere all'utente");
                assertNotNull(rs.getString("state_order"));
            }
        }

        // verifica decremento stock
        int qtyAfter = getStock(shopId, productId, size);
        assertEquals(qtyBefore - buyQty, qtyAfter, "Lo stock non è stato decrementato correttamente");
    }

    // --- helpers ---
    private int getStock(int shopId, int productId, String size) throws Exception {
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement ps = c.prepareStatement(
                     """
                     SELECT quantity
                     FROM product_availability
                     WHERE id_shop=? AND product_id=? AND size=?
                     """)) {
            ps.setInt(1, shopId);
            ps.setInt(2, productId);
            ps.setString(3, size);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Riga stock non trovata");
                return rs.getInt(1);
            }
        }
    }
}
