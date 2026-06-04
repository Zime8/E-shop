package org.example.dao.db;

import org.example.dao.ProductRepository;
import org.example.database.DatabaseConnection;
import org.example.models.entity.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbProductDAO implements ProductRepository {
    private static final Logger logger = Logger.getLogger(DbProductDAO.class.getName());

    @Override
    public List<Product> findLatest(int limit) {
        String call = "{ call sp_find_latest(?) }";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, limit);
            try (ResultSet rs = cs.executeQuery()) {
                List<Product> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e, () -> "Errore durante findLatest");
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<Product> findById(long productId) {
        String sql = """
        SELECT p.product_id,
            p.name_p,
            p.sport,
            p.brand,
            p.category,
            p.image_data,
            p.created_at,
            s.id_shop AS id_shop,
            s.name_s AS shop_name,
            pa.price AS price,
            pa.size AS size
        FROM products p
        JOIN product_availability pa ON pa.product_id = p.product_id
        JOIN shops s ON s.id_shop = pa.id_shop
        WHERE p.product_id = ?
        ORDER BY pa.price ASC, pa.size ASC
        LIMIT 1
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = mapRow(rs);
                    return p != null ? Optional.of(p) : Optional.empty();
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "findById failed: " + productId, e);
            return Optional.empty();
        }
    }


    @Override
    public List<Product> searchByName(String name) {
        List<Product> products = new ArrayList<>();
        String call = "{ call sp_search_by_name(?) }";
        try {
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, name);
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        Product p = mapRow(rs);
                        if (p != null) {
                            products.add(p);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante searchByName", e);
            return Collections.emptyList();
        }
        return products;
    }

    @Override
    public List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                         double minPrice, double maxPrice) {

        List<Product> products = new ArrayList<>();
        try{
            String call = "{ call sp_search_by_filters(?, ?, ?, ?, ?, ?) }";

            String sportVal    = blankToNull(sport);
            String brandVal    = blankToNull(brand);
            String categoryVal = blankToNull(category);
            Integer shopId     = resolveShopId(shop);
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                if (sportVal == null) cs.setNull(1, Types.VARCHAR); else cs.setString(1, sportVal);
                if (brandVal == null) cs.setNull(2, Types.VARCHAR); else cs.setString(2, brandVal);
                if (shopId   == null) cs.setNull(3, Types.INTEGER); else cs.setInt(3, shopId);
                if (categoryVal == null) cs.setNull(4, Types.VARCHAR); else cs.setString(4, categoryVal);
                cs.setDouble(5, minPrice);
                cs.setDouble(6, maxPrice);

                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        Product p = mapRow(rs);
                        if (p != null) {
                            products.add(p);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante searchByFilters", e);
            return Collections.emptyList();
        }
        return products;
    }

    @Override
    public int getShopIdByName(String shopName){
        try{
            String call = "{ call sp_get_shop_id_by_name(?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, shopName);
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) return rs.getInt("id_shop");
                    throw new SQLException("Shop not found: " + shopName);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante getShopByName", e);
            return -1;
        }
    }

    @Override
    public List<String> getAvailableSizes(long productId, int idShop){
        try{
            String call = "{ call sp_get_available_sizes(?, ?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setLong(1, productId);
                cs.setInt(2, idShop);
                try (ResultSet rs = cs.executeQuery()) {
                    List<String> sizes = new ArrayList<>();
                    while (rs.next()) sizes.add(rs.getString("size"));
                    return sizes;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante getAvailableSizes", e);
            return Collections.emptyList();
        }
    }

    @Override
    public double getPriceFor(long productId, int idShop, String size){
        try{
            String call = "{ call sp_get_price_for(?, ?, ?, ?) }";
            try (Connection conn = DatabaseConnection.getConnection();
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
        }catch (SQLException e){
            logger.log(Level.SEVERE, "Errore durante getPriceFor", e);
            return 0.0;
        }
    }

    @Override
    public Integer getStockFor(long productId, int shopId, String size){
        try{
            String call = "{ call sp_get_stock_for(?, ?, ?, ?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setLong(1, productId);
                cs.setInt(2, shopId);
                cs.setString(3, size);
                cs.registerOutParameter(4, Types.INTEGER);
                cs.execute();
                int qty = cs.getInt(4);
                return cs.wasNull() ? 0 : qty;
            }
        }catch (SQLException e){
            logger.log(Level.SEVERE, "Errore durante getStockFor", e);
            return 0;
        }
    }

    @Override
    public boolean existsWish(String username, long productId, int shopId, String size){
        try {
            String call = "{ call sp_exists_wish(?, ?, ?, ?, ?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, username);
                cs.setLong(2, productId);
                cs.setInt(3, shopId);
                if (size == null) cs.setNull(4, Types.VARCHAR); else cs.setString(4, size);
                cs.registerOutParameter(5, Types.TINYINT);
                cs.execute();
                return cs.getByte(5) == 1;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante existsWish", e);
            return false;
        }
    }

    // helper
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    private Product mapRow(ResultSet rs) {
        try {
            long productId = rs.getLong("product_id");
            int idShop = rs.getInt("id_shop");
            String name = rs.getString("name_p");
            String sport = rs.getString("sport");
            String brand = rs.getString("brand");
            String category = rs.getString("category");
            String nameShop = rs.getString("shop_name");
            BigDecimal price = rs.getBigDecimal("price");
            String size = rs.getString("size");
            byte[] imageData = rs.getBytes("image_data");
            LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null;

            return new Product(productId, idShop, name, sport, brand, category,
                    nameShop, price, size, imageData, createdAt);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "mapRow failed", e);
            return null;
        }
    }

    private Integer resolveShopId(String shop) {
        if (shop == null || shop.isBlank()) return null;
        return getShopIdByName(shop.trim());
    }
}
