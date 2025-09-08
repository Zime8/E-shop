package org.example.dao.db;

import org.example.dao.api.ProductDao;
import org.example.database.DatabaseConnection;
import org.example.models.Product;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDaoDb implements ProductDao {
    private static final Logger logger = Logger.getLogger(ProductDaoDb.class.getName());

    @Override
    public List<Product> findLatest(int limit) {
        String call = "{ call sp_find_latest(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, limit);
            try (ResultSet rs = cs.executeQuery()) {
                List<Product> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante findLatest", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Product> searchByName(String name) throws SQLException {
        List<Product> products = new ArrayList<>();
        String call = "{ call sp_search_by_name(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, name);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        }
        return products;
    }

    @Override
    public List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                         double minPrice, double maxPrice) throws SQLException {
        List<Product> products = new ArrayList<>();
        String call = "{ call sp_search_by_filters(?, ?, ?, ?, ?, ?) }";

        String sportVal    = blankToNull(sport);
        String brandVal    = blankToNull(brand);
        String categoryVal = blankToNull(category);
        Integer shopId     = resolveShopId(shop);

        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            // MySQL: setNull con tipo corretto
            if (sportVal == null) cs.setNull(1, Types.VARCHAR); else cs.setString(1, sportVal);
            if (brandVal == null) cs.setNull(2, Types.VARCHAR); else cs.setString(2, brandVal);
            if (shopId   == null) cs.setNull(3, Types.INTEGER); else cs.setInt(3, shopId);
            if (categoryVal == null) cs.setNull(4, Types.VARCHAR); else cs.setString(4, categoryVal);
            cs.setDouble(5, minPrice);
            cs.setDouble(6, maxPrice);

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        }
        return products;
    }

    @Override
    public int getShopIdByName(String shopName) throws SQLException {
        String call = "{ call sp_get_shop_id_by_name(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, shopName);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) return rs.getInt("id_shop");
                throw new SQLException("Shop not found: " + shopName);
            }
        }
    }

    @Override
    public List<String> getAvailableSizes(long productId, int idShop) throws SQLException {
        String call = "{ call sp_get_available_sizes(?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setLong(1, productId);
            cs.setInt(2, idShop);
            try (ResultSet rs = cs.executeQuery()) {
                List<String> sizes = new ArrayList<>();
                while (rs.next()) sizes.add(rs.getString("size"));
                return sizes;
            }
        }
    }

    @Override
    public double getPriceFor(long productId, int idShop, String size) throws SQLException {
        String call = "{ call sp_get_price_for(?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setLong(1, productId);
            cs.setInt(2, idShop);
            cs.setString(3, size);
            cs.registerOutParameter(4, Types.DOUBLE);
            cs.execute();
            double price = cs.getDouble(4);
            if (cs.wasNull()) throw new SQLException("Prezzo non trovato per size=" + size);
            return price;
        }
    }

    @Override
    public Integer getStockFor(long productId, int shopId, String size) throws SQLException {
        String call = "{ call sp_get_stock_for(?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setLong(1, productId);
            cs.setInt(2, shopId);
            cs.setString(3, size);
            cs.registerOutParameter(4, Types.INTEGER);
            cs.execute();
            int qty = cs.getInt(4);
            return cs.wasNull() ? 0 : qty;
        }
    }

    @Override
    public boolean existsWish(String username, long productId, int shopId, String size) throws SQLException {
        String call = "{ call sp_exists_wish(?, ?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            cs.setLong(2, productId);
            cs.setInt(3, shopId);
            if (size == null) cs.setNull(4, Types.VARCHAR); else cs.setString(4, size);
            cs.registerOutParameter(5, Types.TINYINT);
            cs.execute();
            return cs.getByte(5) == 1;
        }
    }

    // helper
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getLong("product_id"));
        p.setName(rs.getString("name_p"));
        p.setSport(rs.getString("sport"));
        p.setBrand(rs.getString("brand"));
        p.setNameShop(rs.getString("shop_name"));
        p.setCategory(rs.getString("category"));
        p.setPrice(rs.getDouble("price"));
        p.setIdShop(rs.getInt("id_shop"));
        byte[] imgBytes = rs.getBytes("image_data");
        p.setImageData(imgBytes);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        return p;
    }

    private Integer resolveShopId(String shop) throws SQLException {
        if (shop == null || shop.isBlank()) return null;
        return getShopIdByName(shop.trim());
    }
}
