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

    private static final String SEARCH_BY_FILTERS_SQL = """
        SELECT p.product_id, p.name_p, p.sport, p.brand, p.category,
               MIN(pa.price) AS price, p.image_data, p.created_at, s.name_s AS shop_name, s.id_shop
        FROM products p
        JOIN product_availability pa ON p.product_id = pa.product_id
        JOIN shops s ON pa.id_shop = s.id_shop
        WHERE pa.price BETWEEN ? AND ?
          AND ( ? IS NULL OR p.sport = ? )
          AND ( ? IS NULL OR p.brand = ? )
          AND ( ? IS NULL OR s.id_shop = ? )
          AND ( ? IS NULL OR p.category = ? )
        GROUP BY p.product_id, p.name_p, p.sport, p.brand, p.category, p.image_data, p.created_at, s.name_s, s.id_shop
        ORDER BY p.created_at DESC
        """;

    @Override
    public List<Product> findLatest(int limit) {
        String sql = """
            SELECT p.product_id, p.name_p, p.sport, p.brand, p.category,
                   pa.price AS price, p.image_data, p.created_at,
                   s.name_s AS shop_name, pa.quantity, pa.size, s.id_shop
            FROM products p
            JOIN product_availability pa ON pa.product_id = p.product_id
            JOIN shops s ON s.id_shop = pa.id_shop
            LEFT JOIN product_availability pa2
              ON pa2.product_id = pa.product_id AND pa2.id_shop = pa.id_shop
             AND (pa2.price < pa.price OR (pa2.price = pa.price AND pa2.size < pa.size))
            WHERE pa2.product_id IS NULL AND pa.quantity > 0
            ORDER BY p.created_at DESC, p.product_id DESC, s.id_shop ASC
            LIMIT ?""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
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
        String sql = """
            SELECT p.product_id, p.name_p, p.sport, p.brand, p.category,
                   MIN(pa.price) AS price, p.image_data, p.created_at, s.name_s AS shop_name, s.id_shop
            FROM products p
            JOIN product_availability pa ON p.product_id = pa.product_id
            JOIN shops s ON pa.id_shop = s.id_shop
            WHERE LOWER(p.name_p) LIKE ?
            GROUP BY p.product_id, p.name_p, p.sport, p.brand, p.category, p.image_data, p.created_at, s.name_s, s.id_shop
            ORDER BY p.created_at DESC""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + (name == null ? "" : name.toLowerCase()) + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        }
        return products;
    }

    @Override
    public List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                         double minPrice, double maxPrice) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sportVal    = blankToNull(sport);
        String brandVal    = blankToNull(brand);
        String categoryVal = blankToNull(category);
        Integer shopId     = resolveShopId(shop);

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_FILTERS_SQL)) {
            bindFilters(stmt, minPrice, maxPrice, sportVal, brandVal, shopId, categoryVal);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        }
        return products;
    }

    @Override public int getShopIdByName(String shopName) throws SQLException {
        String sql = """
            SELECT id_shop
            FROM shops
            WHERE name_s = ?""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shopName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id_shop");
                throw new SQLException("Shop not found: " + shopName);
            }
        }
    }

    @Override
    public List<String> getAvailableSizes(long productId, int idShop) throws SQLException {
        String sql = """
            SELECT size
            FROM product_availability
            WHERE product_id = ? AND id_shop = ? AND quantity > 0
            ORDER BY size ASC""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setInt(2, idShop);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> sizes = new ArrayList<>();
                while (rs.next()) sizes.add(rs.getString("size"));
                return sizes;
            }
        }
    }

    @Override
    public double getPriceFor(long productId, int idShop, String size) throws SQLException {
        String sql = """
            SELECT price
            FROM product_availability
            WHERE product_id=? AND id_shop=? AND size=?""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setInt(2, idShop);
            ps.setString(3, size);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
                throw new SQLException("Prezzo non trovato per size=" + size);
            }
        }
    }

    @Override
    public Integer getStockFor(long productId, int shopId, String size) throws SQLException {
        String sql = """
            SELECT quantity
            FROM product_availability
            WHERE product_id=? AND id_shop=? AND size=?""";
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setInt(2, shopId);
            ps.setString(3, size);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public boolean existsWish(String username, long productId, int shopId, String size) throws SQLException {
        String sql = """
            SELECT 1
            FROM wishlist
            WHERE username = ? AND product_id = ? AND id_shop = ?
              AND ( ? IS NULL OR p_size = ? )
            LIMIT 1
            """;
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, username);
            ps.setLong(i++, productId);
            ps.setInt(i++, shopId);
            if (size == null) {
                ps.setNull(i++, Types.VARCHAR); ps.setNull(i, Types.VARCHAR);
            } else {
                ps.setString(i++, size); ps.setString(i, size);
            }
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ---- helper / mapping (copiati dal tuo DAO statico) ----
    private static void bindFilters(PreparedStatement stmt, double minPrice, double maxPrice,
                                    String sportVal, String brandVal, Integer shopId, String categoryVal) throws SQLException {
        int i = 1;
        stmt.setDouble(i++, minPrice);
        stmt.setDouble(i++, maxPrice);
        i = bindOptionalPair(stmt, i, sportVal);
        i = bindOptionalPair(stmt, i, brandVal);
        i = bindOptionalPair(stmt, i, shopId);
        bindOptionalPair(stmt, i, categoryVal);
    }
    private static int bindOptionalPair(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) { ps.setNull(idx++, Types.VARCHAR); ps.setNull(idx++, Types.VARCHAR); }
        else { ps.setString(idx++, v); ps.setString(idx++, v); }
        return idx;
    }
    private static int bindOptionalPair(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) { ps.setNull(idx++, Types.INTEGER); ps.setNull(idx++, Types.INTEGER); }
        else { ps.setInt(idx++, v); ps.setInt(idx++, v); }
        return idx;
    }
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
