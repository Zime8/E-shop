package org.example.dao;

import org.example.database.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SellerDAO {

    private SellerDAO() {}

    private static final String COL_PRODUCT_ID = "product_id";
    private static final String COL_NAME       = "name_p";
    private static final String COL_SPORT      = "sport";
    private static final String COL_BRAND      = "brand";
    private static final String COL_CATEGORY   = "category";

    // DTO/Record

    public record CatalogRow(
            int productId, String name, String sport, String brand,
            String category, String size, BigDecimal price, int quantity) {}

    public record ShopOrderSummary(
            int orderId, Timestamp orderDate, String state,
            BigDecimal total, String customer, String address) {}

    public record ShopOrderLine(
            long productId, String productName, String size,
            int quantity, BigDecimal unitPrice) {
        public BigDecimal subtotal() {
            return unitPrice == null ? BigDecimal.ZERO :
                    unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public record SellerShop(int shopId, String shopName) {}

    public record ProductOption(
            int productId, String name, String brand,
            String sport, String category) {
        @Override public String toString() {
            return name + " · " + brand + " · " + sport + " (" + category + ")";
        }
    }

    // Utility

    public static SellerShop findShopForUser(int userId) throws SQLException {
        final String call = "{ call sp_seller_find_shop(?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setInt(1, userId);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return new SellerShop(rs.getInt("id_shop"), rs.getString("name_s"));
                }
                return null;
            }
        }
    }

    // Catalogo

    public static List<CatalogRow> listCatalog(int shopId, String search) throws SQLException {
        final String call = "{ call sp_seller_list_catalog(?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setInt(1, shopId);
            cs.setString(2, (search == null || search.isBlank()) ? null : search.trim());
            try (ResultSet rs = cs.executeQuery()) {
                List<CatalogRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new CatalogRow(
                            rs.getInt(COL_PRODUCT_ID),
                            rs.getString(COL_NAME),
                            rs.getString(COL_SPORT),
                            rs.getString(COL_BRAND),
                            rs.getString(COL_CATEGORY),
                            rs.getString("size"),
                            rs.getBigDecimal("price"),
                            rs.getInt("quantity")
                    ));
                }
                return out;
            }
        }
    }

    public static void upsertCatalogRow(int shopId, int productId, String size, BigDecimal price, int qty) throws SQLException {
        Objects.requireNonNull(size, "size");
        final String call = "{ call sp_seller_upsert_catalog(?, ?, ?, ?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setInt(1, shopId);
            cs.setInt(2, productId);
            cs.setString(3, size);
            cs.setBigDecimal(4, price);
            cs.setInt(5, qty);
            cs.executeUpdate();
        }
    }

    public static void updateCatalogRow(int shopId, int productId, String size, BigDecimal price, int qty) throws SQLException {
        final String call = "{ call sp_seller_update_catalog(?, ?, ?, ?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setInt(1, shopId);
            cs.setInt(2, productId);
            cs.setString(3, size);
            cs.setBigDecimal(4, price);
            cs.setInt(5, qty);
            cs.executeUpdate();
        }
    }

    public static void deleteCatalogRow(int shopId, int productId, String size) throws SQLException {
        final String call = "{ call sp_seller_delete_catalog(?, ?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setInt(1, shopId);
            cs.setInt(2, productId);
            cs.setString(3, size);
            cs.executeUpdate();
        }
    }

    public static List<ProductOption> listAllProductOptions() throws SQLException {
        final String call = "{ call sp_seller_list_all_product_options() }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call);
             ResultSet rs = cs.executeQuery()) {
            List<ProductOption> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new ProductOption(
                        rs.getInt(COL_PRODUCT_ID),
                        rs.getString(COL_NAME),
                        rs.getString(COL_BRAND),
                        rs.getString(COL_SPORT),
                        rs.getString(COL_CATEGORY)
                ));
            }
            return out;
        }
    }

    public static List<ProductOption> listProductOptionsByNameLike(String query, int limit) throws SQLException {
        final String call = "{ call sp_seller_list_product_options_by_name(?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setString(1, query == null ? "" : query.trim());
            cs.setInt(2, Math.max(1, limit));
            try (ResultSet rs = cs.executeQuery()) {
                List<ProductOption> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ProductOption(
                            rs.getInt(COL_PRODUCT_ID),
                            rs.getString(COL_NAME),
                            rs.getString(COL_BRAND),
                            rs.getString(COL_SPORT),
                            rs.getString(COL_CATEGORY)
                    ));
                }
                return out;
            }
        }
    }

    // Ordini per shop

    public static List<ShopOrderSummary> listShopOrders(int shopId, String stateFilter) throws SQLException {
        final String call = "{ call sp_seller_list_shop_orders(?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setInt(1, shopId);
            cs.setString(2, (stateFilter == null || stateFilter.isBlank()) ? null : stateFilter.trim());
            try (ResultSet rs = cs.executeQuery()) {
                List<ShopOrderSummary> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ShopOrderSummary(
                            rs.getInt("id_order"),
                            rs.getTimestamp("date_order"),
                            rs.getString("state_order"),
                            rs.getBigDecimal("total"),
                            rs.getString("customer"),
                            rs.getString("address")
                    ));
                }
                return out;
            }
        }
    }

    public static List<ShopOrderLine> listShopOrderLines(int shopId, int orderId) throws SQLException {
        final String call = "{ call sp_seller_list_shop_order_lines(?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setInt(1, shopId);
            cs.setInt(2, orderId);
            try (ResultSet rs = cs.executeQuery()) {
                List<ShopOrderLine> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ShopOrderLine(
                            rs.getLong("id_product"),
                            rs.getString("product_name"),
                            rs.getString("size"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")
                    ));
                }
                return out;
            }
        }
    }

    public static void updateOrderState(int orderId, String newState) throws SQLException {
        final String call = "{ call sp_seller_update_order_state(?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setInt(1, orderId);
            cs.setString(2, newState);
            cs.executeUpdate();
        }
    }
}
