package org.example.dao;

import javafx.scene.image.Image;
import org.example.database.DatabaseConnection;
import org.example.demo.DemoData;
import org.example.models.Product;
import org.example.models.User;
import org.example.util.Session;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {

    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

    public enum LoginStatus { SUCCESS, INVALID_CREDENTIALS, ERROR }
    public record LoginResult(LoginStatus status, Integer userId, String role) {}

    /* =======================
       AUTH / PROFILO
       ======================= */

    public static LoginResult checkLogin(String username, String password) {
        if (Session.isDemo()) {
            try {
                DemoData.ensureLoaded();
                DemoData.User u = DemoData.USERS.get(username);
                if (u == null) return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);
                boolean ok = BCrypt.checkpw(password, u.passHash());
                return ok
                        ? new LoginResult(LoginStatus.SUCCESS, u.id(), u.role())
                        : new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore login (demo)", e);
                return new LoginResult(LoginStatus.ERROR, null, null);
            }
        }

        String sql = """
        SELECT id_user, pass, rol
        FROM users
        WHERE username = ?""";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next())
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);

                int userId = rs.getInt("id_user");
                String passFromDb = rs.getString("pass");
                String role = rs.getString("rol");

                if (passFromDb == null)
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);

                boolean ok = passFromDb.startsWith("$2a$") || passFromDb.startsWith("$2b$") || passFromDb.startsWith("$2y$")
                        ? BCrypt.checkpw(password, passFromDb)
                        : password.equals(passFromDb);

                if (!ok)
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);

                // upgrade a bcrypt se necessario
                if (!passFromDb.startsWith("$2")) {
                    String newHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                    try (PreparedStatement up = conn.prepareStatement("UPDATE users SET pass = ? WHERE id_user = ?")) {
                        up.setString(1, newHash);
                        up.setInt(2, userId);
                        up.executeUpdate();
                    }
                }
                return new LoginResult(LoginStatus.SUCCESS, userId, role);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il check login", e);
            return new LoginResult(LoginStatus.ERROR, null, null);
        }
    }

    public boolean registerUser(String username, String password, String role, String email, String phone) {
        if (Session.isDemo()) {
            try {
                DemoData.ensureLoaded();
                if (DemoData.USERS.containsKey(username)) return false;
                boolean emailTaken = DemoData.USERS.values().stream()
                        .anyMatch(u -> email != null && email.equalsIgnoreCase(u.email()));
                if (emailTaken) return false;

                int newId = DemoData.USERS.size() + 100;
                String hashedPwd = BCrypt.hashpw(password, BCrypt.gensalt(12));
                DemoData.USERS.put(username, new DemoData.User(newId, username, hashedPwd, role, email, phone));
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore registrazione (demo)", e);
                return false;
            }
        }

        String sql = """
        INSERT INTO users (username, pass, rol, email, phone)
        VALUES (?, ?, ?, ?, ?)""";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String hashedPwd = BCrypt.hashpw(password, BCrypt.gensalt(12));
            stmt.setString(1, username);
            stmt.setString(2, hashedPwd);
            stmt.setString(3, role);
            stmt.setString(4, email);
            stmt.setString(5, phone);

            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante la registrazione utente", e);
            return false;
        }
    }

    public boolean isUsernameTaken(String username) {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            return DemoData.USERS.containsKey(username);
        }

        String sql = """
        SELECT username
        FROM users
        WHERE username = ?""";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo username esistente", e);
            return false;
        }
    }

    public boolean isEmailTaken(String email) {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            return DemoData.USERS.values().stream()
                    .anyMatch(u -> u.email() != null && u.email().equalsIgnoreCase(email));
        }

        String sql = """
        SELECT email
        FROM users
        WHERE email = ?""";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo email esistente", e);
            return false;
        }
    }

    public static Integer findUserIdByUsername(String username) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.User u = DemoData.USERS.get(username);
            return u != null ? u.id() : null;
        }

        final String sql = """
        SELECT id_user
        FROM users
        WHERE username = ?""";

        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public static User findByUsername(String username) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.User du = DemoData.USERS.get(username);
            if (du == null) return null;
            User u = new User();
            u.setUsername(du.username());
            u.setEmail(du.email());
            u.setPhone(du.phone());
            u.setPasswordHash(du.passHash());
            return u;
        }

        String sql = """
        SELECT username, email, phone, pass
        FROM users
        WHERE username = ?
        """;
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User u = new User();
                u.setUsername(rs.getString("username"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                u.setPasswordHash(rs.getString("pass"));
                return u;
            }
        }
    }

    public static void updateProfile(String currentUsername, String newUsername, String email, String phone) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.User old = DemoData.USERS.get(currentUsername);
            if (old == null) return;

            // se cambia username, controlla unicità e migra wishlist
            if (!Objects.equals(currentUsername, newUsername)) {
                if (DemoData.USERS.containsKey(newUsername)) {
                    throw new SQLException("Username già esistente (demo)");
                }
                // migra wishlist legata all'username
                List<Product> wl = DemoData.WISHLISTS.remove(currentUsername);
                if (wl != null) DemoData.WISHLISTS.put(newUsername, wl);

                // rimuovi vecchia entry utente (chiave: username)
                DemoData.USERS.remove(currentUsername);
            }

            DemoData.USERS.put(newUsername, new DemoData.User(
                    old.id(),
                    newUsername,
                    old.passHash(),
                    old.role(),
                    email,
                    phone
            ));
            return;
        }

        String sql = "UPDATE users SET username = ?, email = ?, phone = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, currentUsername);
            ps.executeUpdate();
        }
    }

    public static void updateProfileWithPassword(String currentUsername, String newUsername,
                                                 String email, String phone, String plainPassword) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.User old = DemoData.USERS.get(currentUsername);
            if (old == null) return;

            String hashedPwd = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

            if (!Objects.equals(currentUsername, newUsername)) {
                if (DemoData.USERS.containsKey(newUsername)) {
                    throw new SQLException("Username già esistente (demo)");
                }
                List<Product> wl = DemoData.WISHLISTS.remove(currentUsername);
                if (wl != null) DemoData.WISHLISTS.put(newUsername, wl);
                DemoData.USERS.remove(currentUsername);
            }

            DemoData.USERS.put(newUsername, new DemoData.User(
                    old.id(),
                    newUsername,
                    hashedPwd,
                    old.role(),
                    email,
                    phone
            ));
            return;
        }

        String hashedPwd = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        String sql = "UPDATE users SET username = ?, email = ?, phone = ?, pass = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, hashedPwd);
            ps.setString(5, currentUsername);
            ps.executeUpdate();
        }
    }

    /* =======================
       WISHLIST
       ======================= */

    public static void addInWishList(String username, long productId, int idShop, String pSize) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            String key = DemoData.prodKey(productId, idShop, pSize);
            Product p = DemoData.PRODUCTS.get(key);
            if (p == null) {
                // placeholder sensato se il seed non contiene la combinazione
                p = new Product();
                p.setProductId((int) productId);
                p.setIdShop(idShop);
                p.setSize(pSize);
                p.setName("Prodotto #" + productId);
                p.setNameShop("Shop #" + idShop);
                p.setBrand("Demo");
                p.setSport("N/D");
                p.setCategory("N/D");
                p.setPrice(0.0);
            }
            DemoData.WISHLISTS.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());

            final long pid  = p.getProductId();
            final int shop = p.getIdShop();
            final String sz = p.getSize();

            // sostituisci se già presente stessa tripla (product, shop, size)
            DemoData.WISHLISTS.get(username).removeIf(ex ->
                    ex.getProductId() == pid &&
                            ex.getIdShop() == shop &&
                            Objects.equals(ex.getSize(), sz));
            DemoData.WISHLISTS.get(username).add(p);
            return;
        }

        String sql = """
        INSERT IGNORE INTO wishlist(username, product_id, id_shop, p_size)
        VALUES(?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE p_size = VALUES(p_size)
        """;
        runUpdate(sql, ps -> {
            ps.setString(1, username);
            ps.setLong  (2, productId);
            ps.setInt   (3, idShop);
            ps.setString(4, pSize);
        });
    }

    public static void removeInWishlist(String username, long productId, int idShop, String pSize) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            List<Product> list = DemoData.WISHLISTS.getOrDefault(username, Collections.emptyList());
            list.removeIf(p -> p.getProductId() == productId &&
                    p.getIdShop() == idShop &&
                    Objects.equals(p.getSize(), pSize));
            return;
        }

        String sql = """
        DELETE FROM wishlist
        WHERE username = ? AND product_id = ? AND id_shop = ? AND p_size = ?
        """;
        runUpdate(sql, ps -> {
            ps.setString(1, username);
            ps.setLong  (2, productId);
            ps.setInt   (3, idShop);
            ps.setString(4, pSize);
        });
    }

    public static void clearWishlist(String username) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.WISHLISTS.remove(username);
            return;
        }

        String sql = "DELETE FROM wishlist WHERE username = ?";
        runUpdate(sql, ps -> ps.setString(1, username));
    }

    public static List<Product> getFavorites(String username) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            return new ArrayList<>(DemoData.WISHLISTS.getOrDefault(username, Collections.emptyList()));
        }

        String sql = """
            SELECT p.product_id, p.name_p, p.sport, p.brand, p.category,
                   w.id_shop, w.p_size, s.name_s, pa.price, p.image_data
            FROM wishlist w
            JOIN products p ON p.product_id = w.product_id
            JOIN shops s    ON s.id_shop = w.id_shop
            LEFT JOIN product_availability pa
              ON pa.product_id = w.product_id
             AND pa.id_shop    = w.id_shop
             AND pa.size       = w.p_size
            WHERE w.username = ?""";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            List<Product> favs = new ArrayList<>();
            while (rs.next()) {
                favs.add(mapRowToProduct(rs));
            }
            return favs;
        }
    }

    /* =======================
       UTIL DB
       ======================= */

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private static void runUpdate(String sql, SqlBinder binder) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
        }
    }

    private static Product mapRowToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setName(rs.getString("name_p"));
        p.setSport(rs.getString("sport"));
        p.setBrand(rs.getString("brand"));
        p.setIdShop(rs.getInt("id_shop"));
        p.setNameShop(rs.getString("name_s"));
        p.setPrice(rs.getDouble("price"));
        p.setSize(rs.getString("p_size"));

        byte[] imgBytes = rs.getBytes("image_data");
        if (imgBytes != null && imgBytes.length > 0) {
            Image img = new Image(new ByteArrayInputStream(imgBytes));
            if (img.isError()) {
                logger.log(Level.SEVERE, "Errore caricamento immagine", img.getException());
            } else {
                logger.log(Level.INFO, "Image pronta: {0}×{1}", new Object[]{img.getWidth(), img.getHeight()});
            }
            p.setImage(img);
        }
        return p;
    }
}
