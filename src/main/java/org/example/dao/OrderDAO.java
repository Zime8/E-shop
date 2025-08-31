package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.models.CartItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class OrderDAO {

    // Impedisci l'istanziazione (utility class)
    private OrderDAO() {
        throw new AssertionError("Utility class, no instances allowed");
    }

    // === SQL ===
    private static final String INSERT_ORDER_SQL =
            "INSERT INTO orders_client (id_user, date_order, date_order_update, state_order) " +
                    "VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'in elaborazione')";

    private static final String INSERT_DETAIL_SQL =
            "INSERT INTO details_order (id_order, id_product, id_shop, size, quantity, price) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String LOCK_STOCK_SQL = """
        SELECT quantity
        FROM product_availability
        WHERE product_id = ? AND id_shop = ? AND size = ?
        FOR UPDATE
        """;

    private static final String UPDATE_STOCK_SQL = """
        UPDATE product_availability
        SET quantity = quantity - ?
        WHERE product_id = ? AND id_shop = ? AND size = ?
        """;

    public record CreationResult(
            List<Integer> orderIds,
            Map<Integer, Integer> shopToOrderId
    ) {
        // Canonical constructor per copiare difensivamente (immutabilità di riferimento)
        public CreationResult {
            orderIds = List.copyOf(orderIds);
            shopToOrderId = Map.copyOf(shopToOrderId);
        }
    }


    /**
     * Crea tanti ordini quanti sono gli shop presenti nella lista items.
     * Ogni ordine avrà le sue righe in details_order.
     */
    private static CreationResult createOrdersPerShop(Connection conn, int userId, List<CartItem> items) throws SQLException {
        // Manteniamo l'ordine di primo incontro degli shop per allinearlo alle generated keys
        final Map<Integer, List<CartItem>> byShop = new LinkedHashMap<>();
        for (CartItem it : items) {
            byShop.computeIfAbsent(it.getShopId(), k -> new ArrayList<>()).add(it);
        }

        final List<Integer> createdOrderIds = new ArrayList<>();
        final Map<Integer, Integer> shopToOrderId = new HashMap<>();
        final List<Integer> shopIdsInOrder = new ArrayList<>(byShop.keySet());

        try (PreparedStatement psOrder = conn.prepareStatement(INSERT_ORDER_SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psDetail = conn.prepareStatement(INSERT_DETAIL_SQL)) {

            // userId è invariante: set una volta e aggiungi al batch per ogni shop
            psOrder.setInt(1, userId);
            for (int i = 0; i < shopIdsInOrder.size(); i++) {
                psOrder.addBatch();
            }

            // Esegui tutti gli INSERT in una sola chiamata
            psOrder.executeBatch();

            // Allinea generated keys agli shop nella stessa sequenza
            try (ResultSet rsKeys = psOrder.getGeneratedKeys()) {
                int idx = 0;
                while (rsKeys.next()) {
                    if (idx >= shopIdsInOrder.size()) break;
                    int orderId = rsKeys.getInt(1);
                    int shopId  = shopIdsInOrder.get(idx++);
                    createdOrderIds.add(orderId);
                    shopToOrderId.put(shopId, orderId);
                }
                if (idx != shopIdsInOrder.size()) {
                    throw new SQLException("Numero di chiavi generate non corrisponde al numero di ordini creati.");
                }
            }

            // Inserisci TUTTI i dettagli in batch (uno per ogni item, associato al suo orderId)
            for (int shopId : shopIdsInOrder) {
                int orderId = shopToOrderId.get(shopId);

                psDetail.setInt(1, orderId);
                psDetail.setInt(3, shopId);
                for (CartItem it : byShop.get(shopId)) {
                    psDetail.setLong(2, it.getProductId());
                    psDetail.setString(4, it.getSize());
                    psDetail.setInt(5, it.getQuantity());
                    psDetail.setDouble(6, it.getUnitPrice());
                    psDetail.addBatch();
                }
            }
            psDetail.executeBatch(); // unica esecuzione
            psDetail.clearBatch();
        }

        return new CreationResult(createdOrderIds, shopToOrderId);
    }

    private static void decrementStockForItems(Connection conn, List<CartItem> items) throws SQLException {
        // aggrega quantità per chiave (productId|shopId|size) mantenendo l'ordine d'inserimento
        final Map<String, Integer> need = new LinkedHashMap<>();
        for (CartItem it : items) {
            final String key = it.getProductId() + "|" + it.getShopId() + "|" + it.getSize();
            need.merge(key, it.getQuantity(), Integer::sum);
        }

        try (PreparedStatement psLock = conn.prepareStatement(LOCK_STOCK_SQL);
             PreparedStatement psUpd  = conn.prepareStatement(UPDATE_STOCK_SQL)) {

            // 1) Lock + Validazione disponibilità
            for (Map.Entry<String, Integer> e : need.entrySet()) {
                final String[] parts = e.getKey().split("\\|");
                final long   productId = Long.parseLong(parts[0]);
                final int    shopId    = Integer.parseInt(parts[1]);
                final String size      = parts[2];
                final int    qtyNeeded = e.getValue();

                psLock.setLong(1, productId);
                psLock.setInt(2, shopId);
                psLock.setString(3, size);

                final int available;
                try (ResultSet rs = psLock.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Stock non trovato per product=" + productId +
                                ", shop=" + shopId + ", size=" + size);
                    }
                    available = rs.getInt(1);
                }

                if (available < qtyNeeded) {
                    throw new SQLException("Stock insufficiente per product=" + productId +
                            ", shop=" + shopId + ", size=" + size +
                            " (richiesto " + qtyNeeded + ", disponibile " + available + ")");
                }

                // 2) Prepara l'UPDATE in batch
                psUpd.setInt(1, qtyNeeded);
                psUpd.setLong(2, productId);
                psUpd.setInt(3, shopId);
                psUpd.setString(4, size);
                psUpd.addBatch();
            }

            // 3) Esegui tutti gli update in una volta
            psUpd.executeBatch();
            psUpd.clearBatch();
        }
    }

    public static CreationResult placeOrderWithStockDecrement(int userId, List<CartItem> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Lista articoli vuota");
        }

        final Connection conn = DatabaseConnection.getInstance();
        final boolean oldAuto = conn.getAutoCommit();
        CreationResult result;
        SQLException toThrow = null;

        try {
            conn.setAutoCommit(false);

            // Crea ordini per shop
            result = createOrdersPerShop(conn, userId, items);

            // Scala lo stock
            decrementStockForItems(conn, items);

            conn.commit();
            return result;

        } catch (SQLException e) {
            // rollback per errori SQL
            try { conn.rollback(); } catch (SQLException rbEx) { e.addSuppressed(rbEx); }
            toThrow = e; // rimanderemo dopo il finally
        } catch (RuntimeException e) {
            // rollback anche per errori runtime
            try { conn.rollback(); } catch (SQLException rbEx) { e.addSuppressed(rbEx); }
            // mantieni la semantica: rilancia come SQLException (metodo dichiara throws SQLException)
            toThrow = new SQLException("Errore durante la transazione ordine/stock", e);
        } catch (Exception e) {
            // qualunque altra checked (improbabile qui, ma sicuro)
            try { conn.rollback(); } catch (SQLException rbEx) { e.addSuppressed(rbEx); }
            toThrow = new SQLException("Errore generico durante la transazione ordine/stock", e);
        } finally {
            try {
                conn.setAutoCommit(oldAuto);
            } catch (SQLException e) {
                if (toThrow != null) {
                    toThrow.addSuppressed(e);
                } else {
                    // nessuna eccezione principale: propaga il problema di ripristino
                    toThrow = e;
                }
            }
        }

        // se siamo qui, c'è un'eccezione da rilanciare
        throw toThrow;
    }

    // ======== LETTURA ORDINI / DETTAGLI ========

    /** Tutti gli ordini dell'utente con totale calcolato dalle righe (quantity*price). */
    public static List<OrderSummary> getOrdersByUser(int userId) throws SQLException {
        final String SQL = """
            SELECT
                o.id_order AS id_order,
                o.date_order AS date_order,
                o.state_order AS state_order,
                COALESCE(SUM(d.quantity * d.price), 0) AS total_amount
            FROM orders_client o
            LEFT JOIN details_order d ON d.id_order = o.id_order
            WHERE o.id_user = ?
            GROUP BY o.id_order, o.date_order, o.state_order
            ORDER BY o.date_order DESC, o.id_order DESC
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(SQL)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderSummary> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new OrderSummary(
                            rs.getInt("id_order"),
                            rs.getTimestamp("date_order"),
                            rs.getString("state_order"),
                            rs.getBigDecimal("total_amount")
                    ));
                }
                return list;
            }
        }
    }

    /** Righe di un ordine con nome prodotto (products.name_p) e nome negozio (shops.name_s). */
    public static List<OrderLine> getOrderItems(int orderId) throws SQLException {
        final String SQL = """
            SELECT
                d.id_order,
                d.id_product,
                d.id_shop,
                d.size,
                d.quantity,
                d.price,              -- prezzo unitario al momento dell'ordine
                p.name_p AS product_name,
                s.name_s AS shop_name
            FROM details_order d
            JOIN products p ON p.product_id = d.id_product
            JOIN shops    s ON s.id_shop    = d.id_shop
            WHERE d.id_order = ?
            ORDER BY p.name_p ASC, d.id_product ASC
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(SQL)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderLine> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new OrderLine(
                            rs.getInt("id_order"),
                            rs.getLong("id_product"),
                            rs.getInt("id_shop"),
                            rs.getString("product_name"),
                            rs.getString("shop_name"),
                            rs.getString("size"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")
                    ));
                }
                return list;
            }
        }
    }

    // ======== DTO per la UI ========

    /**
     * Riga tabella ORDINI
     */
    public record OrderSummary(int idOrder, Timestamp dateOrder, String stateOrder, BigDecimal totalAmount) { }

    /**
     * Riga tabella DETTAGLIO
     */
        public record OrderLine(int orderId, long productId, int shopId, String productName, String shopName, String size, int quantity, BigDecimal unitPrice) {
            public BigDecimal getSubtotal() {
                return (unitPrice == null) ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
            }
        }

}
