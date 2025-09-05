package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.demo.DemoData;
import org.example.models.CartItem;
import org.example.models.Order;
import org.example.models.OrderStatus;
import org.example.models.Product;
import org.example.util.Session;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class OrderDAO {

    private OrderDAO() {
        throw new AssertionError("Utility class, no instances allowed");
    }

    // === SQL (produzione) ===
    private static final String INSERT_ORDER_SQL = """
        INSERT INTO orders_client (id_user, date_order, date_order_update, state_order)
        VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'in elaborazione')""";

    private static final String INSERT_DETAIL_SQL = """
        INSERT INTO details_order (id_order, id_product, id_shop, size, quantity, price)
        VALUES (?, ?, ?, ?, ?, ?)""";

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
        public CreationResult {
            orderIds = List.copyOf(orderIds);
            shopToOrderId = Map.copyOf(shopToOrderId);
        }
    }

    /* ============================================================
       UTIL DEMO
       ============================================================ */
    private static String stockKey(long productId, int shopId, String size) {
        return DemoData.stockKey(productId, shopId, size);
    }

    private static void ensureDemoSeed() {
        DemoData.ensureLoaded();
        // Inizializza stock demo se mancante (es. 5 per variante)
        for (Product p : DemoData.PRODUCTS.values()) {
            DemoData.STOCK.putIfAbsent(stockKey(p.getProductId(), p.getIdShop(), p.getSize()), 5);
        }
    }

    /* ============================================================
       CREAZIONE ORDINI (con gestione stock)
       ============================================================ */
    // == ENTRY POINT semplificato ==
    public static CreationResult placeOrderWithStockDecrement(int userId, List<CartItem> items) throws SQLException {
        validateItems(items);
        return Session.isDemo()
                ? placeOrderDemo(userId, items)
                : placeOrderDb(userId, items);
    }

    /* ========================== DEMO ========================== */

    private static CreationResult placeOrderDemo(int userId, List<CartItem> items) throws SQLException {
        ensureDemoSeed();

        // 1) aggrega fabbisogno e 2) valida stock
        Map<String, Integer> need = aggregateNeed(items);
        validateDemoStock(need);

        // 3) crea ordini per shop
        Map<Integer, List<CartItem>> byShop = groupByShop(items);
        CreationResult res = createDemoOrders(userId, byShop);

        // 4) scala stock
        decrementDemoStock(need);
        return res;
    }

    private static Map<String, Integer> aggregateNeed(List<CartItem> items) {
        Map<String, Integer> need = new LinkedHashMap<>();
        for (CartItem it : items) {
            String key = stockKey(it.getProductId(), it.getShopId(), it.getSize());
            need.merge(key, it.getQuantity(), Integer::sum);
        }
        return need;
    }

    private static void validateDemoStock(Map<String, Integer> need) throws SQLException {
        for (var e : need.entrySet()) {
            int available = DemoData.STOCK.getOrDefault(e.getKey(), 0);
            int required = e.getValue();
            if (available < required) {
                String[] parts = e.getKey().split("\\|");
                long pid = Long.parseLong(parts[0]);
                int sid  = Integer.parseInt(parts[1]);
                String sz = parts[2];
                throw new SQLException("Stock insufficiente (demo) per product=" + pid
                        + ", shop=" + sid + ", size=" + sz
                        + " (richiesto " + required + ", disponibile " + available + ")");
            }
        }
    }

    private static Map<Integer, List<CartItem>> groupByShop(List<CartItem> items) {
        Map<Integer, List<CartItem>> byShop = new LinkedHashMap<>();
        for (CartItem it : items) {
            byShop.computeIfAbsent(it.getShopId(), k -> new ArrayList<>()).add(it);
        }
        return byShop;
    }

    private static CreationResult createDemoOrders(int userId, Map<Integer, List<CartItem>> byShop) {
        List<Integer> createdIds = new ArrayList<>();
        Map<Integer, Integer> shopToOrderId = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (var entry : byShop.entrySet()) {
            int shopId = entry.getKey();
            List<CartItem> group = entry.getValue();

            int orderId = DemoData.DEMO_ORDER_ID.incrementAndGet();
            shopToOrderId.put(shopId, orderId);
            createdIds.add(orderId);

            Order ord = new Order(orderId, userId, now, org.example.models.OrderStatus.IN_ELABORAZIONE);
            for (CartItem it : group) {
                String prodKey = DemoData.prodKey(it.getProductId(), it.getShopId(), it.getSize());
                Product p = DemoData.PRODUCTS.get(prodKey);
                String productName = (p != null) ? p.getName()     : ("Prodotto #" + it.getProductId());
                String shopName    = (p != null) ? p.getNameShop() : ("Shop #" + it.getShopId());

                ord.addLine(new org.example.models.OrderLine(
                        orderId,
                        it.getProductId(),
                        it.getShopId(),
                        productName,
                        shopName,
                        it.getSize(),
                        it.getQuantity(),
                        java.math.BigDecimal.valueOf(it.getUnitPrice())
                ));
            }
            DemoData.ORDERS
                    .computeIfAbsent(userId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(ord);
        }
        return new CreationResult(createdIds, shopToOrderId);
    }

    private static void decrementDemoStock(Map<String, Integer> need) {
        for (var e : need.entrySet()) {
            int available = DemoData.STOCK.getOrDefault(e.getKey(), 0);
            DemoData.STOCK.put(e.getKey(), available - e.getValue());
        }
    }

    /* ======================== PRODUZIONE ======================== */

    private static CreationResult placeOrderDb(int userId, List<CartItem> items) throws SQLException {
        Connection conn = DatabaseConnection.getInstance();
        boolean oldAuto = conn.getAutoCommit();
        SQLException toThrow = null;

        try {
            conn.setAutoCommit(false);
            CreationResult result = createOrdersPerShop(conn, userId, items);
            decrementStockForItems(conn, items);
            conn.commit();
            return result;

        } catch (SQLException e) {
            rollbackQuiet(conn, e);
            toThrow = e;
        } catch (RuntimeException e) {
            rollbackQuiet(conn, e);
            toThrow = new SQLException("Errore durante la transazione ordine/stock", e);
        } catch (Exception e) {
            rollbackQuiet(conn, e);
            toThrow = new SQLException("Errore generico durante la transazione ordine/stock", e);
        } finally {
            try { conn.setAutoCommit(oldAuto); }
            catch (SQLException e) { if (toThrow != null) toThrow.addSuppressed(e); else toThrow = e; }
        }
        throw toThrow;
    }

    private static void rollbackQuiet(Connection conn, Exception cause) {
        try { conn.rollback(); } catch (SQLException rbEx) { cause.addSuppressed(rbEx); }
    }

    /* ======================== VALIDAZIONE ======================== */

    private static void validateItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Lista articoli vuota");
        }
    }

    // ======== HELPER DB (originali) ========

    private static CreationResult createOrdersPerShop(Connection conn, int userId, List<CartItem> items) throws SQLException {
        final Map<Integer, List<CartItem>> byShop = new LinkedHashMap<>();
        for (CartItem it : items) {
            byShop.computeIfAbsent(it.getShopId(), k -> new ArrayList<>()).add(it);
        }

        final List<Integer> createdOrderIds = new ArrayList<>();
        final Map<Integer, Integer> shopToOrderId = new HashMap<>();
        final List<Integer> shopIdsInOrder = new ArrayList<>(byShop.keySet());

        try (PreparedStatement psOrder = conn.prepareStatement(INSERT_ORDER_SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psDetail = conn.prepareStatement(INSERT_DETAIL_SQL)) {

            psOrder.setInt(1, userId);
            for (int i = 0; i < shopIdsInOrder.size(); i++) {
                psOrder.addBatch();
            }
            psOrder.executeBatch();

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
            psDetail.executeBatch();
            psDetail.clearBatch();
        }

        return new CreationResult(createdOrderIds, shopToOrderId);
    }

    private static void decrementStockForItems(Connection conn, List<CartItem> items) throws SQLException {
        final Map<String, Integer> need = new LinkedHashMap<>();
        for (CartItem it : items) {
            final String key = it.getProductId() + "|" + it.getShopId() + "|" + it.getSize();
            need.merge(key, it.getQuantity(), Integer::sum);
        }

        try (PreparedStatement psLock = conn.prepareStatement(LOCK_STOCK_SQL);
             PreparedStatement psUpd  = conn.prepareStatement(UPDATE_STOCK_SQL)) {

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

                psUpd.setInt(1, qtyNeeded);
                psUpd.setLong(2, productId);
                psUpd.setInt(3, shopId);
                psUpd.setString(4, size);
                psUpd.addBatch();
            }

            psUpd.executeBatch();
            psUpd.clearBatch();
        }
    }

    // ======== LETTURA ORDINI / DETTAGLI (compatibilità esistente) ========

    /** Riga tabella ORDINI */
    public record OrderSummary(int idOrder, Timestamp dateOrder, String stateOrder, BigDecimal totalAmount) { }

    /** Riga tabella DETTAGLIO (attenzione: nome collide con il model, qui è record interno) */
    public record OrderLine(int orderId, long productId, int shopId, String productName, String shopName, String size, int quantity, BigDecimal unitPrice) {
        public BigDecimal getSubtotal() {
            return (unitPrice == null) ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    // ======== NUOVI METODI comodi: restituiscono direttamente i MODEL ========

    /** Ordini completi (con righe) come model `Order`. */
    public static List<Order> listOrdersModel(int userId) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            var src = DemoData.ORDERS.getOrDefault(userId, Collections.emptyList());
            // ritorna copia profonda
            List<Order> out = new ArrayList<>(src.size());
            for (Order o : src) {
                Order copy = new Order(o.getId(), o.getUserId(), o.getCreatedAt(), o.getStatus());
                for (org.example.models.OrderLine l : o.getLines()) {
                    copy.addLine(new org.example.models.OrderLine(
                            l.getOrderId(), l.getProductId(), l.getShopId(),
                            l.getProductName(), l.getShopName(), l.getSize(),
                            l.getQuantity(), l.getUnitPrice()
                    ));
                }
                out.add(copy);
            }
            out.sort((a, b) -> {
                int c = b.getCreatedAt().compareTo(a.getCreatedAt());
                return (c != 0) ? c : Integer.compare(b.getId(), a.getId());
            });
            return out;
        }

        // PRODUZIONE: header + righe
        final String H_SQL = """
            SELECT id_order, id_user, date_order, state_order
            FROM orders_client
            WHERE id_user = ?
            ORDER BY date_order DESC, id_order DESC
        """;
        final String L_SQL_TMPL = """
            SELECT d.id_order, d.id_product, d.id_shop, d.size, d.quantity, d.price,
                   p.name_p AS product_name, s.name_s AS shop_name
            FROM details_order d
            JOIN products p ON p.product_id = d.id_product
            JOIN shops    s ON s.id_shop    = d.id_shop
            WHERE d.id_order IN (%s)
            ORDER BY d.id_order ASC, p.name_p ASC, d.id_product ASC
        """;

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement psH = conn.prepareStatement(H_SQL)) {

            psH.setInt(1, userId);
            List<Order> orders = new ArrayList<>();
            try (ResultSet rs = psH.executeQuery()) {
                while (rs.next()) {
                    orders.add(new Order(
                            rs.getInt("id_order"),
                            rs.getInt("id_user"),
                            rs.getTimestamp("date_order").toLocalDateTime(),
                            OrderStatus.fromDb(rs.getString("state_order"))
                    ));
                }
            }
            if (orders.isEmpty()) return orders;

            String in = String.join(",", Collections.nCopies(orders.size(), "?"));
            try (PreparedStatement psL = conn.prepareStatement(L_SQL_TMPL.formatted(in))) {
                int idx = 1;
                for (Order o : orders) psL.setInt(idx++, o.getId());

                try (ResultSet rs = psL.executeQuery()) {
                    Map<Integer, List<org.example.models.OrderLine>> grouped = new HashMap<>();
                    while (rs.next()) {
                        org.example.models.OrderLine l = new org.example.models.OrderLine(
                                rs.getInt("id_order"),
                                rs.getLong("id_product"),
                                rs.getInt("id_shop"),
                                rs.getString("product_name"),
                                rs.getString("shop_name"),
                                rs.getString("size"),
                                rs.getInt("quantity"),
                                rs.getBigDecimal("price")
                        );
                        grouped.computeIfAbsent(l.getOrderId(), k -> new ArrayList<>()).add(l);
                    }
                    for (Order o : orders) {
                        o.getLines().addAll(grouped.getOrDefault(o.getId(), List.of()));
                    }
                }
            }
            return orders;
        }
    }

}
