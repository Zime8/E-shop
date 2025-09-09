package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.demo.DemoData;
import org.example.models.*;
import org.example.util.Session;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class OrderDAO {

    private static final String ORDER_ID = "id_order";

    private OrderDAO() {
        throw new AssertionError("Utility class, no instances allowed");
    }

    // Risultato creazione ordini
    public record CreationResult(
            List<Integer> orderIds,
            Map<Integer, Integer> shopToOrderId
    ) {
        public CreationResult {
            orderIds = List.copyOf(orderIds);
            shopToOrderId = Map.copyOf(shopToOrderId);
        }
    }

    // UTIL DEMO
    private static String stockKey(long productId, int shopId, String size) {
        return DemoData.stockKey(productId, shopId, size);
    }

    private static void ensureDemoSeed() {
        DemoData.ensureLoaded();
        // Inizializza stock demo se mancante (es. 5 per variante)
        for (Product p : DemoData.products().values()) {
            DemoData.stock().putIfAbsent(stockKey(p.getProductId(), p.getIdShop(), p.getSize()), 5);
        }
    }

    // ENTRY POINT CREAZIONE ORDINE
    public static CreationResult placeOrderWithStockDecrement(int userId, List<CartItem> items, String address) throws SQLException {
        validateItems(items);
        return Session.isDemo()
                ? placeOrderDemo(userId, items)
                : placeOrderDb(userId, items, address);
    }

    // DEMO
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
            int available = DemoData.stock().getOrDefault(e.getKey(), 0);
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
                Product p = DemoData.products().get(prodKey);
                String productName = (p != null) ? p.getName()     : ("Prodotto #" + it.getProductId());
                String shopName    = (p != null) ? p.getNameShop() : ("Shop #" + it.getShopId());

                ord.addLine(new org.example.models.OrderLine(
                        orderId,
                        it.getProductId(),
                        it.getShopId(),
                        new org.example.models.OrderLine.Details(
                                productName,
                                shopName,
                                it.getSize(),
                                it.getQuantity(),
                                BigDecimal.valueOf(it.getUnitPrice())
                        )
                ));
            }
            DemoData.orders()
                    .computeIfAbsent(userId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(ord);
        }
        return new CreationResult(createdIds, shopToOrderId);
    }

    private static void decrementDemoStock(Map<String, Integer> need) {
        for (var e : need.entrySet()) {
            int available = DemoData.stock().getOrDefault(e.getKey(), 0);
            DemoData.stock().put(e.getKey(), available - e.getValue());
        }
    }

    // PRODUZIONE

    // Escape per JSON
    private static String jsonEscape(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String buildItemsJson(List<CartItem> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            CartItem it = items.get(i);
            if (i > 0) sb.append(',');
            sb.append('{')
                    .append("\"productId\":").append(it.getProductId()).append(',')
                    .append("\"shopId\":").append(it.getShopId()).append(',')
                    .append("\"size\":").append(jsonEscape(it.getSize())).append(',')
                    .append("\"quantity\":").append(it.getQuantity()).append(',')
                    .append("\"unitPrice\":").append(it.getUnitPrice())
                    .append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    // ENTRY POINT DB (refactor: bassa complessità)
    private static CreationResult placeOrderDb(int userId, List<CartItem> items, String address) throws SQLException {
        final String CALL = "{ call sp_place_order(?, ?, ?) }";

        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(CALL)) {

            boolean oldAuto = beginTx(conn);
            try {
                bindPlaceOrderParams(cs, userId, address, items);
                Map<Integer, Integer> shopToOrder = executeAndReadMapping(cs);
                conn.commit();
                return toCreationResult(shopToOrder);
            } catch (Exception ex) {
                safeRollback(conn);
                throw wrapToSqlException(ex);
            } finally {
                restoreAutoCommit(conn, oldAuto);
            }
        }
    }

/* =======================
   Helpers di supporto
   ======================= */

    private static boolean beginTx(Connection conn) throws SQLException {
        boolean old = conn.getAutoCommit();
        conn.setAutoCommit(false);
        return old;
    }

    private static void restoreAutoCommit(Connection conn, boolean oldAuto) {
        try {
            conn.setAutoCommit(oldAuto);
        } catch (Exception ignore) { /* no-op */ }
    }

    private static void safeRollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception ignore) { /* no-op */ }
    }

    private static void bindPlaceOrderParams(CallableStatement cs, int userId, String address, List<CartItem> items) throws SQLException {
        cs.setInt(1, userId);
        if (address == null || address.isBlank()) cs.setNull(2, Types.VARCHAR);
        else cs.setString(2, address);
        cs.setString(3, buildItemsJson(items));
    }

    /** Esegue la SP, avanza tra i (possibili) resultset intermedi e ritorna la mappa shop->orderId. */
    private static Map<Integer, Integer> executeAndReadMapping(CallableStatement cs) throws SQLException {
        boolean hasInitialResultSet = cs.execute();
        if (!hasInitialResultSet && !advanceToFinalResultSet(cs)) {
            throw new SQLException("sp_place_order non ha restituito il result set atteso (id_shop/id_order).");
        }

        // Se siamo qui, o c'era già un RS, o advanceToFinalResultSet l'ha posizionato sul finale
        try (ResultSet rs = cs.getResultSet()) {
            return readShopOrderMapping(rs);
        }
    }

    /** Avanza nei risultati finché non trova un ResultSet con colonne id_order/order_id. */
    private static boolean advanceToFinalResultSet(CallableStatement cs) throws SQLException {
        boolean has = true; // entrerà nel ciclo con il next getMoreResults()
        while (true) {
            has = cs.getMoreResults();
            if (has) {
                try (ResultSet probe = cs.getResultSet()) {
                    if (isFinalMappingResult(probe)) {
                        // non chiudere il RS finale: dobbiamo rileggerlo dal caller
                        // Riapriamo con getResultSet() fuori dal try-with-resources
                        return true;
                    }
                }
            } else if (cs.getUpdateCount() == -1) {
                return false; // fine stream risultati
            }
        }
    }

    /** Determina se il ResultSet corrente è quello con le colonne finali (id_shop/id_order o shop_id/order_id). */
    private static boolean isFinalMappingResult(ResultSet rs) throws SQLException {
        if (rs == null) return false;
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String label = md.getColumnLabel(i);
            if (ORDER_ID.equalsIgnoreCase(label) || ORDER_ID.equalsIgnoreCase(label)) {
                return true;
            }
        }
        return false;
    }

    /** Legge la mappa shop->orderId dal RS finale e la ritorna. */
    private static Map<Integer, Integer> readShopOrderMapping(ResultSet rs) throws SQLException {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        while (rs.next()) {
            // gestiamo entrambi i naming possibili (id_shop/shop_id e id_order/order_id)
            int shopId  = getIntByAliases(rs, "id_shop", "shop_id");
            int orderId = getIntByAliases(rs, ORDER_ID, "order_id");
            map.put(shopId, orderId);
        }
        if (map.isEmpty()) {
            throw new SQLException("Result set finale vuoto: atteso elenco (id_shop, id_order).");
        }
        return map;
    }

    /** Ritorna rs.getInt sul primo alias presente. */
    private static int getIntByAliases(ResultSet rs, String primary, String alias) throws SQLException {
        try {
            return rs.getInt(primary);
        } catch (SQLException ex) {
            // prova con alias
            return rs.getInt(alias);
        }
    }

    /** Converte la mappa in CreationResult (ordinando gli orderIds per stabilità). */
    private static CreationResult toCreationResult(Map<Integer, Integer> shopToOrder) {
        List<Integer> orderIds = new ArrayList<>(shopToOrder.values());
        orderIds.sort(Integer::compareTo);
        return new CreationResult(orderIds, shopToOrder);
    }

    /** Uniforma le eccezioni a SQLException (come il metodo originale). */
    private static SQLException wrapToSqlException(Exception ex) {
        if (ex instanceof SQLException se) return se;
        return new SQLException("Errore durante placeOrderDb", ex);
    }


    // VALIDAZIONE
    private static void validateItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Lista articoli vuota");
        }
    }

    // LETTURA ORDINI

    // Riga tabella ORDINI
    public record OrderSummary(int idOrder, Timestamp dateOrder, String stateOrder, BigDecimal totalAmount) { }

    // Riga tabella DETTAGLIO
    public record OrderLine(int orderId, long productId, int shopId, String productName, String shopName, String size, int quantity, BigDecimal unitPrice) {
        public BigDecimal getSubtotal() {
            return (unitPrice == null) ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    // Ordini completi come model `Order`
    public static List<Order> listOrdersModel(int userId) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            var src = DemoData.orders().getOrDefault(userId, Collections.emptyList());
            // ritorna copia profonda
            List<Order> out = new ArrayList<>(src.size());
            for (Order o : src) {
                Order copy = new Order(o.getId(), o.getUserId(), o.getCreatedAt(), o.getStatus());
                for (org.example.models.OrderLine l : o.getLines()) {
                    copy.addLine(new org.example.models.OrderLine(
                            l.getOrderId(), l.getProductId(), l.getShopId(),
                            new org.example.models.OrderLine.Details(
                                    l.getProductName(),
                                    l.getShopName(),
                                    l.getSize(),
                                    l.getQuantity(),
                                    l.getUnitPrice()
                            )
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

        // PRODUZIONE con stored procedure
        final String CALL_H = "{ call sp_list_orders_header(?) }";
        final String CALL_L = "{ call sp_list_orders_lines(?) }";

        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement csH = conn.prepareCall(CALL_H)) {

            csH.setInt(1, userId);
            List<Order> orders = new ArrayList<>();
            Map<Integer, Order> byId = new LinkedHashMap<>();

            try (ResultSet rs = csH.executeQuery()) {
                while (rs.next()) {
                    int idOrder = rs.getInt(ORDER_ID);
                    Order ord = new Order(
                            idOrder,
                            rs.getInt("id_user"),
                            rs.getTimestamp("date_order").toLocalDateTime(),
                            OrderStatus.fromDb(rs.getString("state_order"))
                    );
                    orders.add(ord);
                    byId.put(idOrder, ord);
                }
            }
            if (orders.isEmpty()) return orders;

            try (CallableStatement csL = conn.prepareCall(CALL_L)) {
                csL.setInt(1, userId);
                try (ResultSet rs = csL.executeQuery()) {
                    while (rs.next()) {
                        int orderId = rs.getInt(ORDER_ID);
                        org.example.models.OrderLine line = new org.example.models.OrderLine(
                                orderId,
                                rs.getLong("id_product"),
                                rs.getInt("id_shop"),
                                new org.example.models.OrderLine.Details(
                                        rs.getString("product_name"),
                                        rs.getString("shop_name"),
                                        rs.getString("size"),
                                        rs.getInt("quantity"),
                                        rs.getBigDecimal("price")
                                )
                        );
                        Order o = byId.get(orderId);
                        if (o != null) o.getLines().add(line);
                    }
                }
            }
            return orders;
        }
    }

}