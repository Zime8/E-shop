package org.example.dao.db;

import org.example.dao.OrderRepository;
import org.example.dao.RepositoryException;
import org.example.dao.support.OrderValidation;
import org.example.database.DatabaseConnection;
import org.example.models.entity.*;

import java.sql.*;
import java.util.*;

public final class DbOrderDAO implements OrderRepository {

    private static final String ORDER_ID = "id_order";

    // CREAZIONE ORDINE
    @Override
    public CreationResult placeOrderWithStockDecrement(int userId, List<CartItem> items, String address) {

        OrderValidation.validateItems(items);

        try {
            return placeOrderDb(userId, items, address);
        } catch (SQLException e) {
            throw new RepositoryException(
                    "Errore durante la creazione dell'ordine per userId=" + userId,
                    e
            );
        }
    }

    // Ordini completi come model `Order`
    @Override
    public List<Order> listOrdersModel(int userId) {

        // PRODUZIONE
        final String CALL_H = "{ call sp_list_orders_header(?) }";
        final String CALL_L = "{ call sp_list_orders_lines(?) }";

        try (Connection conn = DatabaseConnection.getConnection();
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
                        OrderLine line = new OrderLine(
                                orderId,
                                rs.getLong("id_product"),
                                rs.getInt("id_shop"),
                                new OrderLine.Details(
                                        rs.getString("product_name"),
                                        rs.getString("shop_name"),
                                        rs.getString("size"),
                                        rs.getInt("quantity"),
                                        rs.getBigDecimal("price")
                                )
                        );
                        Order o = byId.get(orderId);
                        if (o != null) o.addLine(line);
                    }
                }
            }
            return orders;
        } catch (SQLException e) {
            throw new RepositoryException(
                    "Errore durante il recupero degli ordini per userId=" + userId,
                    e);
        }
    }

    // Escape per JSON
    private String jsonEscape(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String buildItemsJson(List<CartItem> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            CartItem it = items.get(i);
            if (i > 0) sb.append(',');
            sb.append('{')
                    .append("\"productId\":").append(it.productId()).append(',')
                    .append("\"shopId\":").append(it.shopId()).append(',')
                    .append("\"size\":").append(jsonEscape(it.size())).append(',')
                    .append("\"quantity\":").append(it.quantity()).append(',')
                    .append("\"unitPrice\":").append(it.unitPrice())
                    .append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    // DB
    private CreationResult placeOrderDb(int userId, List<CartItem> items, String address) throws SQLException {
        final String CALL = "{ call sp_place_order(?, ?, ?) }";

        try (Connection conn = DatabaseConnection.getConnection();
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

    // Helpers

    private boolean beginTx(Connection conn) throws SQLException {
        boolean old = conn.getAutoCommit();
        conn.setAutoCommit(false);
        return old;
    }

    private void restoreAutoCommit(Connection conn, boolean oldAuto) {
        try {
            conn.setAutoCommit(oldAuto);
        } catch (Exception ignore) {
            // Nessuna operazione
        }
    }

    private void safeRollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception ignore) {
            // Nessuna operazione
        }
    }

    private void bindPlaceOrderParams(CallableStatement cs, int userId, String address, List<CartItem> items) throws SQLException {
        cs.setInt(1, userId);
        if (address == null || address.isBlank()) cs.setNull(2, Types.VARCHAR);
        else cs.setString(2, address);
        cs.setString(3, buildItemsJson(items));
    }

    // Esegue la SP, avanza tra i resultset intermedi e ritorna la mappa shop->orderId.
    private Map<Integer, Integer> executeAndReadMapping(CallableStatement cs) throws SQLException {
        boolean hasInitialResultSet = cs.execute();
        if (!hasInitialResultSet && !advanceToFinalResultSet(cs)) {
            throw new SQLException("sp_place_order non ha restituito il result set atteso (id_shop/id_order).");
        }

        try (ResultSet rs = cs.getResultSet()) {
            return readShopOrderMapping(rs);
        }
    }

    // Avanza nei risultati finché non trova un ResultSet con colonne id_order/order_id.
    private boolean advanceToFinalResultSet(CallableStatement cs) throws SQLException {
        boolean has;
        while (true) {
            has = cs.getMoreResults();
            if (has) {
                try (ResultSet probe = cs.getResultSet()) {
                    if (isFinalMappingResult(probe)) {
                        return true;
                    }
                }
            } else if (cs.getUpdateCount() == -1) {
                return false;
            }
        }
    }

    // Determina se il ResultSet corrente è quello con le colonne finali (id_shop/id_order o shop_id/order_id).
    private boolean isFinalMappingResult(ResultSet rs) throws SQLException {
        if (rs == null) return false;
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String label = md.getColumnLabel(i);
            if (ORDER_ID.equalsIgnoreCase(label)) {
                return true;
            }
        }
        return false;
    }

    // Legge la mappa shop->orderId dal RS finale e la ritorna
    private Map<Integer, Integer> readShopOrderMapping(ResultSet rs) throws SQLException {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        while (rs.next()) {
            int shopId  = getIntByAliases(rs, "id_shop", "shop_id");
            int orderId = getIntByAliases(rs, ORDER_ID, "order_id");
            map.put(shopId, orderId);
        }
        if (map.isEmpty()) {
            throw new SQLException("Result set finale vuoto: atteso elenco (id_shop, id_order).");
        }
        return map;
    }

    // Ritorna rs.getInt sul primo alias presente.
    private int getIntByAliases(ResultSet rs, String primary, String alias) throws SQLException {
        try {
            return rs.getInt(primary);
        } catch (SQLException ex) {
            return rs.getInt(alias);
        }
    }

    // Converte la mappa in CreationResult
    private CreationResult toCreationResult(Map<Integer, Integer> shopToOrder) {
        List<Integer> orderIds = new ArrayList<>(shopToOrder.values());
        orderIds.sort(Integer::compareTo);
        return new CreationResult(orderIds, shopToOrder);
    }

    // Uniforma le eccezioni a SQLException
    private SQLException wrapToSqlException(Exception ex) {
        if (ex instanceof SQLException se) return se;
        return new SQLException("Errore durante placeOrderDb", ex);
    }

}