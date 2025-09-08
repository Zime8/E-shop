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

public final class UserDAO {

    private UserDAO() {}

    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

    public enum LoginStatus { SUCCESS, INVALID_CREDENTIALS, ERROR }
    public record LoginResult(LoginStatus status, Integer userId, String role) {}

    // AUTH / PROFILO
    public static LoginResult checkLogin(String username, String password) {
        if (Session.isDemo()) {
            try {
                DemoData.ensureLoaded();
                DemoData.User u = DemoData.users().get(username);
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

        final String call = "{ call sp_user_login(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            try (ResultSet rs = cs.executeQuery()) {
                if (!rs.next())
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);

                int userId = rs.getInt("id_user");
                String passFromDb = rs.getString("pass");
                String role = rs.getString("rol");

                if (passFromDb == null)
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);

                boolean ok = passFromDb.startsWith("$2")
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

    public static boolean registerUser(String username, String password, String role, String email, String phone) {
        if (Session.isDemo()) {
            try {
                DemoData.ensureLoaded();
                if (DemoData.users().containsKey(username)) return false;
                boolean emailTaken = DemoData.users().values().stream()
                        .anyMatch(u -> email != null && email.equalsIgnoreCase(u.email()));
                if (emailTaken) return false;

                int newId = DemoData.users().size() + 100;
                String hashedPwd = BCrypt.hashpw(password, BCrypt.gensalt(12));
                DemoData.users().put(username, new DemoData.User(newId, username, hashedPwd, role, email, phone));
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore registrazione (demo)", e);
                return false;
            }
        }

        final String call = "{ call sp_user_register(?, ?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            String hashedPwd = BCrypt.hashpw(password, BCrypt.gensalt(12));
            cs.setString(1, username);
            cs.setString(2, hashedPwd);
            cs.setString(3, role);
            cs.setString(4, email);
            cs.setString(5, phone);
            cs.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante la registrazione utente", e);
            return false;
        }
    }

    public static boolean isUsernameTaken(String username) {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            return DemoData.users().containsKey(username);
        }

        final String call = "{ call sp_user_check_username(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            try (ResultSet rs = cs.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo username esistente", e);
            return false;
        }
    }

    public static boolean isEmailTaken(String email) {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            return DemoData.users().values().stream()
                    .anyMatch(u -> u.email() != null && u.email().equalsIgnoreCase(email));
        }

        final String call = "{ call sp_user_check_email(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, email);
            try (ResultSet rs = cs.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo email esistente", e);
            return false;
        }
    }

    public static Integer findUserIdByUsername(String username) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.User u = DemoData.users().get(username);
            return u != null ? u.id() : null;
        }

        final String call = "{ call sp_user_find_id(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            try (ResultSet rs = cs.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public static User findByUsername(String username) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.User du = DemoData.users().get(username);
            if (du == null) return null;
            User u = new User();
            u.setUsername(du.username());
            u.setEmail(du.email());
            u.setPhone(du.phone());
            u.setPasswordHash(du.passHash());
            return u;
        }

        final String call = "{ call sp_user_find_by_username(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            try (ResultSet rs = cs.executeQuery()) {
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
            // gestione demo invariata
            DemoData.User old = DemoData.users().get(currentUsername);
            if (old == null) return;
            if (!Objects.equals(currentUsername, newUsername)) {
                if (DemoData.users().containsKey(newUsername)) {
                    throw new SQLException("Username già esistente (demo)");
                }
                List<Product> wl = DemoData.wishlists().remove(currentUsername);
                if (wl != null) DemoData.wishlists().put(newUsername, wl);
                DemoData.users().remove(currentUsername);
            }
            DemoData.users().put(newUsername, new DemoData.User(
                    old.id(), newUsername, old.passHash(), old.role(), email, phone
            ));
            return;
        }

        final String call = "{ call sp_user_update_profile(?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, currentUsername);
            cs.setString(2, newUsername);
            cs.setString(3, email);
            cs.setString(4, phone);
            cs.executeUpdate();
        }
    }

    public static void updateProfileWithPassword(String currentUsername, String newUsername,
                                                 String email, String phone, String plainPassword) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.User old = DemoData.users().get(currentUsername);
            if (old == null) return;
            String hashedPwd = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
            if (!Objects.equals(currentUsername, newUsername)) {
                if (DemoData.users().containsKey(newUsername)) {
                    throw new SQLException("Username già esistente (demo)");
                }
                List<Product> wl = DemoData.wishlists().remove(currentUsername);
                if (wl != null) DemoData.wishlists().put(newUsername, wl);
                DemoData.users().remove(currentUsername);
            }
            DemoData.users().put(newUsername, new DemoData.User(
                    old.id(), newUsername, hashedPwd, old.role(), email, phone
            ));
            return;
        }

        final String call = "{ call sp_user_update_profile_pwd(?, ?, ?, ?, ?) }";
        String hashedPwd = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, currentUsername);
            cs.setString(2, newUsername);
            cs.setString(3, email);
            cs.setString(4, phone);
            cs.setString(5, hashedPwd);
            cs.executeUpdate();
        }
    }

    // WISHLIST
    public static void addInWishList(String username, long productId, int idShop, String pSize) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            String key = DemoData.prodKey(productId, idShop, pSize);
            Product p = DemoData.products().get(key);
            if (p == null) {
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
            DemoData.wishlists().computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
            DemoData.wishlists().get(username).removeIf(ex ->
                    ex.getProductId() == productId &&
                            ex.getIdShop() == idShop &&
                            Objects.equals(ex.getSize(), pSize));
            DemoData.wishlists().get(username).add(p);
            return;
        }

        final String call = "{ call sp_wishlist_add(?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            cs.setLong(2, productId);
            cs.setInt(3, idShop);
            cs.setString(4, pSize);
            cs.executeUpdate();
        }
    }

    public static void removeInWishlist(String username, long productId, int idShop, String pSize) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            List<Product> list = DemoData.wishlists().getOrDefault(username, Collections.emptyList());
            list.removeIf(p -> p.getProductId() == productId &&
                    p.getIdShop() == idShop &&
                    Objects.equals(p.getSize(), pSize));
            return;
        }

        final String call = "{ call sp_wishlist_remove(?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            cs.setLong(2, productId);
            cs.setInt(3, idShop);
            cs.setString(4, pSize);
            cs.executeUpdate();
        }
    }

    public static void clearWishlist(String username) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            DemoData.wishlists().remove(username);
            return;
        }

        final String call = "{ call sp_wishlist_clear(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            cs.executeUpdate();
        }
    }

    public static List<Product> getFavorites(String username) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            return new ArrayList<>(DemoData.wishlists().getOrDefault(username, Collections.emptyList()));
        }

        final String call = "{ call sp_wishlist_get(?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            try (ResultSet rs = cs.executeQuery()) {
                List<Product> favs = new ArrayList<>();
                while (rs.next()) {
                    favs.add(mapRowToProduct(rs));
                }
                return favs;
            }
        }
    }

    // UTIL
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
            if (!img.isError()) {
                p.setImage(img);
            }
        }
        return p;
    }
}
