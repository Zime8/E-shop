package org.example.dao;

import javafx.scene.image.Image;
import org.example.database.DatabaseConnection;
import org.example.models.Product;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDAO {

    private static final Logger logger = Logger.getLogger(ProductDAO.class.getName());

    // Query costante con filtri opzionali parametrici
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
                    ORDER BY p.created_at DESC""";


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
                 AND (pa2.price < pa.price
                      OR (pa2.price = pa.price AND pa2.size < pa.size))
                WHERE pa2.product_id IS NULL
                  AND pa.quantity > 0
                ORDER BY p.created_at DESC, p.product_id DESC, s.id_shop ASC
                LIMIT ?""";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Product> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                    return list;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante findLatest", e);
            return Collections.emptyList();
        }
    }

    public static List<Product> searchByName(String name) throws SQLException {
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

        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + name.toLowerCase() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante la ricerca per nome: {0}", name);
            throw new SQLException("Errore durante la ricerca per nome: " + name, e);
        }

        return products;
    }

    public List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                         double minPrice, double maxPrice) throws SQLException {
        List<Product> products = new ArrayList<>();

        // Calcolo shopId solo se "shop" è non vuoto (stessa logica di prima)
        Integer shopId = null;
        if (shop != null && !shop.isBlank()) {
            shopId = getShopIdByName(shop.trim());
        }

        // Normalizza stringhe vuote a null per i filtri opzionali
        String sportVal    = blankToNull(sport);
        String brandVal    = blankToNull(brand);
        String categoryVal = blankToNull(category);

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_FILTERS_SQL)) {

            int i = 1;
            stmt.setDouble(i++, minPrice);
            stmt.setDouble(i++, maxPrice);

            // sport: ( ? IS NULL OR p.sport = ? )
            if (sportVal == null) { stmt.setNull(i++, Types.VARCHAR); stmt.setNull(i++, Types.VARCHAR); }
            else { stmt.setString(i++, sportVal); stmt.setString(i++, sportVal); }

            // brand: ( ? IS NULL OR p.brand = ? )
            if (brandVal == null) { stmt.setNull(i++, Types.VARCHAR); stmt.setNull(i++, Types.VARCHAR); }
            else { stmt.setString(i++, brandVal); stmt.setString(i++, brandVal); }

            // shopId: ( ? IS NULL OR s.id_shop = ? )
            if (shopId == null)  { stmt.setNull(i++, Types.INTEGER); stmt.setNull(i++, Types.INTEGER); }
            else { stmt.setInt(i++, shopId); stmt.setInt(i++, shopId); }

            // category: ( ? IS NULL OR p.category = ? )
            if (categoryVal == null) { stmt.setNull(i++, Types.VARCHAR); stmt.setNull(i, Types.VARCHAR); }
            else { stmt.setString(i++, categoryVal); stmt.setString(i, categoryVal); }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE,
                    "Errore durante la ricerca per filtri: sport={0}, brand={1}, shop={2}, category={3}, min={4}, max={5}",
                    new Object[]{sport, brand, shop, category, minPrice, maxPrice});
            throw new SQLException(String.format(
                    "Errore durante la ricerca per filtri: %s, %s, %s, %s, %.2f, %.2f",
                    sport, brand, shop, category, minPrice, maxPrice), e);
        }

        return products;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static Product mapRow(ResultSet rs) throws SQLException {
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
        logger.log(Level.INFO, "ImgBytes length for {0} = {1}", new Object[]{p.getProductId(), (imgBytes == null ? 0 : imgBytes.length)});

        if (imgBytes != null && imgBytes.length > 0) {
            Image img = new Image(new ByteArrayInputStream(imgBytes));
            if (img.isError()) {
                logger.log(Level.SEVERE, "Errore caricamento immagine", img.getException());
            } else {
                logger.log(Level.INFO, "Image pronta: {0}×{1}", new Object[]{img.getWidth(), img.getHeight()});
            }
            p.setImage(img);
        }

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            p.setCreatedAt(ts.toLocalDateTime());
        }

        return p;
    }

    public static int getShopIdByName(String shopName) throws SQLException {
        String sql = "SELECT id_shop FROM shops WHERE name_s = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shopName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_shop");
                } else {
                    throw new SQLException("Shop not found: " + shopName);
                }
            }
        }
    }

    // Restituisce le taglie disponibili per (product, shop) con quantità > 0
    public static List<String> getAvailableSizes(long productId, int idShop) throws SQLException {
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

    // Prezzo per specifica taglia
    public static double getPriceFor(long productId, int idShop, String size) throws SQLException {
        String sql = """
                SELECT price
                "FROM product_availability
                "WHERE product_id = ? AND id_shop = ? AND size = ?""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setInt(2, idShop);
            ps.setString(3, size);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
                throw new SQLException("Prezzo non trovato per la taglia " + size);
            }
        }
    }

    // metodo per tornare la massima quantità disponibile del singolo prodotto
    public static Integer getStockFor(long productId, int shopId, String size) throws SQLException {
        String sql = """
        SELECT quantity
        FROM product_availability
        WHERE product_id = ? AND id_shop = ? AND size = ?""";
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


    public static boolean existsWish(String username, long productId, int idShop, String size) throws SQLException {
        String sql = """
                SELECT 1
                FROM wishlist
                WHERE username = ? AND product_id = ? AND id_shop = ? AND p_size = ? LIMIT 1""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, productId);
            ps.setInt(3, idShop);
            ps.setString(4, size);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Restituisce true se esiste già una riga per user+product+shop */
    public static boolean existsWish(String username, long productId, int shopId)
            throws SQLException {
        String sql = """
                SELECT 1
                FROM wishlist
                WHERE username = ? AND product_id = ? AND id_shop = ?
                LIMIT 1""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, productId);
            ps.setInt(3, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

}
