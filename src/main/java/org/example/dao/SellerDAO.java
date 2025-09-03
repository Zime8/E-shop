package org.example.dao;

import org.example.database.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SellerDAO {

    private SellerDAO() {}

    // ---- Column name constants (evita duplicazioni nei ResultSet) ----
    private static final String COL_PRODUCT_ID = "product_id";
    private static final String COL_NAME       = "name_p";
    private static final String COL_SPORT      = "sport";
    private static final String COL_BRAND      = "brand";
    private static final String COL_CATEGORY   = "category";


    /* ============================== DTO/Record ============================== */

    /** Riga di catalogo (join products + product_availability) visibile al venditore */
    public record CatalogRow(
            int productId,
            String name,       // products.name_p
            String sport,
            String brand,
            String category,
            String size,       // product_availability.size
            BigDecimal price,  // product_availability.price
            int quantity       // product_availability.quantity
    ) {}

    /** Ordine del negozio (totale relativo SOLO agli articoli di questo shop) */
    public record ShopOrderSummary(
            int orderId,
            Timestamp orderDate,
            String state,
            BigDecimal total,
            String customer
    ) {}

    /** Riga d’ordine per questo shop */
    public record ShopOrderLine(
            long productId,
            String productName,
            String size,
            int quantity,
            BigDecimal unitPrice
    ) {
        public BigDecimal subtotal() {
            return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /** Info shop del venditore */
    public record SellerShop(int shopId, String shopName) {}

    /** Opzione di prodotto da mostrare nella ComboBox del dialog */
    public record ProductOption(
            int productId,
            String name,
            String brand,
            String sport,
            String category
    ) {
        @Override public String toString() {
            // Come verrà mostrato nella ComboBox
            return name + " · " + brand + " · " + sport + " (" + category + ")";
        }
    }

    /* ============================== Utility lookup ============================== */

    public static SellerShop findShopForUser(int userId) throws SQLException {
        final String sql = """
            SELECT u.id_shop, s.name_s
            FROM users u
            LEFT JOIN shops s ON s.id_shop = u.id_shop
            WHERE u.id_user = ? AND u.rol = 'venditore'
        """;
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return new SellerShop(rs.getInt(1), rs.getString(2));
                }
                return null;
            }
        }
    }

    /* ============================== Catalogo ============================== */

    public static List<CatalogRow> listCatalog(int shopId, String search) throws SQLException {
        final String sql = getString();

        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, shopId);
            // bind per il filtro ripetuto
            String s = (search == null || search.isBlank()) ? null : search.trim();
            ps.setString(2, s);
            ps.setString(3, s);
            ps.setString(4, s);
            ps.setString(5, s);
            ps.setString(6, s);

            List<CatalogRow> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
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
            }
            return out;
        }
    }

    private static String getString() {
        final String base = """
            SELECT pa.product_id, p.name_p, p.sport, p.brand, p.category,
                   pa.size, pa.price, pa.quantity
            FROM product_availability pa
            JOIN products p ON p.product_id = pa.product_id
            WHERE pa.id_shop = ?
        """;
        final String filt = """
            AND (
                 ? IS NULL
                 OR p.name_p    LIKE CONCAT('%', ?, '%')
                 OR p.brand     LIKE CONCAT('%', ?, '%')
                 OR p.category  LIKE CONCAT('%', ?, '%')
                 OR p.sport     LIKE CONCAT('%', ?, '%')
            )
        """;
        return base + filt + " ORDER BY p.name_p ASC, pa.size ASC";
    }

    /** Inserisce/aggiorna una riga catalogo dello shop (chiave: shopId+productId+size). */
    public static void upsertCatalogRow(int shopId, int productId, String size, BigDecimal price, int qty) throws SQLException {
        Objects.requireNonNull(size, "size");
        final String sql = """
            INSERT INTO product_availability (id_shop, product_id, size, price, quantity)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE price = VALUES(price), quantity = VALUES(quantity)
        """;
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setInt(2, productId);
            ps.setString(3, size);
            ps.setBigDecimal(4, price);
            ps.setInt(5, qty);
            ps.executeUpdate();
        }
    }

    public static void updateCatalogRow(int shopId, int productId, String size, BigDecimal price, int qty) throws SQLException {
        final String sql = """
            UPDATE product_availability
               SET price = ?, quantity = ?
             WHERE id_shop = ? AND product_id = ? AND size = ?
        """;
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setBigDecimal(1, price);
            ps.setInt(2, qty);
            ps.setInt(3, shopId);
            ps.setInt(4, productId);
            ps.setString(5, size);
            ps.executeUpdate();
        }
    }

    public static void deleteCatalogRow(int shopId, int productId, String size) throws SQLException {
        final String sql = """
            DELETE FROM product_availability
             WHERE id_shop = ? AND product_id = ? AND size = ?
        """;
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setInt(2, productId);
            ps.setString(3, size);
            ps.executeUpdate();
        }
    }

    /** Elenco completo delle opzioni prodotto (per la ComboBox). */
    public static List<ProductOption> listAllProductOptions() throws SQLException {
        final String sql = """
        SELECT product_id, name_p, brand, sport, category
        FROM products
        ORDER BY name_p
    """;
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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

    /** Ricerca opzioni per nome parziale (type-ahead). */
    public static List<ProductOption> listProductOptionsByNameLike(String query, int limit) throws SQLException {
        final String sql = """
        SELECT product_id, name_p, brand, sport, category
        FROM products
        WHERE name_p LIKE CONCAT('%', ?, '%')
        ORDER BY name_p
        LIMIT ?
    """;
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, query == null ? "" : query.trim());
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
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



    /* ============================== Ordini per shop ============================== */

    public static List<ShopOrderSummary> listShopOrders(int shopId, String stateFilter) throws SQLException {
        final String sql = getSql();

        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, shopId);
            String f = (stateFilter == null || stateFilter.isBlank()) ? null : stateFilter.trim();
            ps.setString(2, f);
            ps.setString(3, f);

            List<ShopOrderSummary> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ShopOrderSummary(
                            rs.getInt("id_order"),
                            rs.getTimestamp("date_order"),
                            rs.getString("state_order"),
                            rs.getBigDecimal("total"),
                            rs.getString("customer")
                    ));
                }
            }
            return out;
        }
    }

    private static String getSql() {
        final String base = """
            SELECT o.id_order,
                   o.date_order,
                   o.state_order,
                   u.username AS customer,
                   COALESCE(SUM(d.quantity * d.price), 0) AS total
            FROM orders_client o
            JOIN details_order d ON d.id_order = o.id_order AND d.id_shop = ?
            JOIN users u ON u.id_user = o.id_user
            WHERE 1=1
        """;
        final String byState = " AND (? IS NULL OR o.state_order = ?)";
        final String tail = " GROUP BY o.id_order, o.date_order, o.state_order, u.username ORDER BY o.date_order DESC, o.id_order DESC";

        return base + byState + tail;
    }

    public static List<ShopOrderLine> listShopOrderLines(int shopId, int orderId) throws SQLException {
        final String sql = """
            SELECT d.id_product,
                p.name_p AS product_name,
                d.quantity,
                d.price,
                d.size
            FROM details_order d
            JOIN products p ON p.product_id = d.id_product
            WHERE d.id_shop = ? AND d.id_order = ?
            ORDER BY p.name_p ASC, d.id_product ASC
        """;
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setInt(2, orderId);
            List<ShopOrderLine> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ShopOrderLine(
                            rs.getLong("id_product"),
                            rs.getString("product_name"),
                            rs.getString("size"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")
                    ));
                }
            }
            return out;
        }
    }

    public static void updateOrderState(int orderId, String newState) throws SQLException {
        final String sql = """
            UPDATE orders_client
               SET state_order = ?, date_order_update = CURRENT_TIMESTAMP
             WHERE id_order = ?
        """;
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, newState);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }
}
