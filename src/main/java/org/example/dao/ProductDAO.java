package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.demo.DemoData;
import org.example.models.Product;
import org.example.util.Session;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProductDAO {

    private ProductDAO() {
        throw new AssertionError("Utility class, no instances allowed");
    }

    private static final Logger logger = Logger.getLogger(ProductDAO.class.getName());
    private static final Comparator<Product> BY_CREATED_DESC_THEN_ID_DESC = (a, b) -> {
        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
            int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
            if (cmp != 0) return cmp;
        }
        return Long.compare(b.getProductId(), a.getProductId());
    };

    // Query costante con filtri opzionali parametrici (solo DB)
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

    public static List<Product> findLatest(int limit) {
        if (Session.isDemo()) {
            try {
                DemoData.ensureLoaded();
                // In demo: ordina per createdAt se disponibile, altrimenti per productId DESC
                List<Product> all = new ArrayList<>(DemoData.products().values());
                all.sort(BY_CREATED_DESC_THEN_ID_DESC);
                return all.stream().limit(Math.max(0, limit)).toList();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore durante findLatest (demo)", e);
                return Collections.emptyList();
            }
        }

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

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Product> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante findLatest", e);
            return Collections.emptyList();
        }
    }

    /* =========================
       SEARCH BY NAME
       ========================= */
    public static List<Product> searchByName(String name) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            String q = name == null ? "" : name.toLowerCase();
            return DemoData.products().values().stream()
                    .filter(p -> {
                        String n = p.getName() == null ? "" : p.getName().toLowerCase();
                        return n.contains(q);
                    })
                    .toList();
        }

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

    public static List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                         double minPrice, double maxPrice) throws SQLException {
        if (Session.isDemo()) {
            return searchByFiltersDemo(sport, brand, shop, category, minPrice, maxPrice);
        }

        String sportVal    = blankToNull(sport);
        String brandVal    = blankToNull(brand);
        String categoryVal = blankToNull(category);
        Integer shopId     = resolveShopId(shop);

        return searchByFiltersDb(sportVal, brandVal, categoryVal, shopId, minPrice, maxPrice);
    }

    private static List<Product> searchByFiltersDemo(String sport, String brand, String shop, String category,
                                              double minPrice, double maxPrice) {
        DemoData.ensureLoaded();

        String sportVal    = blankToNull(sport);
        String brandVal    = blankToNull(brand);
        String categoryVal = blankToNull(category);
        String shopVal     = blankToNull(shop);

        return DemoData.products().values().stream()
                .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                .filter(p -> sportVal == null    || Objects.equals(p.getSport(), sportVal))
                .filter(p -> brandVal == null    || Objects.equals(p.getBrand(), brandVal))
                .filter(p -> categoryVal == null || Objects.equals(p.getCategory(), categoryVal))
                .filter(p -> shopVal == null     || (p.getNameShop() != null && p.getNameShop().equals(shopVal)))
                .sorted(BY_CREATED_DESC_THEN_ID_DESC)
                .toList();
    }

    private static Integer resolveShopId(String shop) throws SQLException {
        if (shop == null || shop.isBlank()) return null;
        return getShopIdByName(shop.trim());
    }

    private static List<Product> searchByFiltersDb(String sportVal, String brandVal, String categoryVal, Integer shopId,
                                            double minPrice, double maxPrice) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_FILTERS_SQL)) {

            bindFilters(stmt, minPrice, maxPrice, sportVal, brandVal, shopId, categoryVal);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE,
                    "Errore durante la ricerca per filtri: sport={0}, brand={1}, shopId={2}, category={3}, min={4}, max={5}",
                    new Object[]{sportVal, brandVal, shopId, categoryVal, minPrice, maxPrice});
            throw new SQLException(String.format(
                    "Errore durante la ricerca per filtri: %s, %s, %s, %s, %.2f, %.2f",
                    sportVal, brandVal, (shopId), categoryVal, minPrice, maxPrice), e);
        }
        return products;
    }

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

    /** Binder generico per ( ? IS NULL OR col = ? ) – String */
    private static int bindOptionalPair(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null) {
            ps.setNull(idx++, Types.VARCHAR);
            ps.setNull(idx++, Types.VARCHAR);
        } else {
            ps.setString(idx++, value);
            ps.setString(idx++, value);
        }
        return idx;
    }

    /** Binder generico per ( ? IS NULL OR col = ? ) – Integer */
    private static int bindOptionalPair(PreparedStatement ps, int idx, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(idx++, Types.INTEGER);
            ps.setNull(idx++, Types.INTEGER);
        } else {
            ps.setInt(idx++, value);
            ps.setInt(idx++, value);
        }
        return idx;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /* =========================
       MAPPING (solo DB)
       ========================= */
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
        logger.log(Level.INFO, "ImgBytes length for {0} = {1}",
                new Object[]{p.getProductId(), (imgBytes == null ? 0 : imgBytes.length)});
        p.setImageData(imgBytes);

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            p.setCreatedAt(ts.toLocalDateTime());
        }
        return p;
    }

    /* =========================
       SHOP ID LOOKUP
       ========================= */
    public static int getShopIdByName(String shopName) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            // Deriva l'id dal seed: prendi un prodotto che appartiene allo shop
            return DemoData.products().values().stream()
                    .filter(p -> p.getNameShop() != null && p.getNameShop().equals(shopName))
                    .findFirst()
                    .map(Product::getIdShop)
                    .orElseThrow(() -> new SQLException("Shop not found (demo): " + shopName));
        }

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

    /* =========================
       SIZES / PRICE / STOCK
       ========================= */
    // Restituisce le taglie disponibili per (product, shop) con quantità > 0
    public static List<String> getAvailableSizes(long productId, int idShop) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            // In demo: tutte le varianti presenti nel seed per (productId, idShop)
            return DemoData.products().values().stream()
                    .filter(p -> p.getProductId() == productId && p.getIdShop() == idShop)
                    .map(Product::getSize)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        }

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
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            String key = DemoData.prodKey(productId, idShop, size);
            Product p = DemoData.products().get(key);
            if (p != null) return p.getPrice();
            throw new SQLException("Prezzo non trovato (demo) per la taglia " + size);
        }

        String sql = """
            SELECT price
            FROM product_availability
            WHERE product_id = ? AND id_shop = ? AND size = ?""";
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

    // Massima quantità disponibile
    public static Integer getStockFor(long productId, int shopId, String size) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            // In demo: se esiste la variante, considera stock "minimo positivo"
            String key = DemoData.prodKey(productId, shopId, size);
            return DemoData.products().containsKey(key) ? 1 : 0;
        }

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

    /* =========================
       WISHLIST CHECK
       ========================= */
    // unico entry-point pubblico: se non ti serve la size, passa null
    public static boolean existsWish(String username, long productId, int shopId, String size)
            throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            List<Product> list = DemoData.wishlists().getOrDefault(username, Collections.emptyList());
            final String sz = size; // cattura effettivamente final
            return list.stream().anyMatch(p ->
                    p.getProductId() == productId &&
                            p.getIdShop() == shopId &&
                            (sz == null || Objects.equals(p.getSize(), sz))
            );
        }

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
                ps.setNull(i++, Types.VARCHAR);
                ps.setNull(i,   Types.VARCHAR);
            } else {
                ps.setString(i++, size);
                ps.setString(i,   size);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // overload comodo per le chiamate senza size
    public static boolean existsWish(String username, long productId, int shopId) throws SQLException {
        return existsWish(username, productId, shopId, null);
    }
}
