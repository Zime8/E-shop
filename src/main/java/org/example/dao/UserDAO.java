package org.example.dao;

import javafx.scene.image.Image;
import org.example.database.DatabaseConnection;
import org.example.models.Product;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {

    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

    public enum LoginStatus { SUCCESS, INVALID_CREDENTIALS, ERROR }
    public record LoginResult(LoginStatus status, Integer userId, String role) {}

    public static LoginResult checkLogin(String username, String password) {
        String sql = "SELECT id_user, pass, rol FROM users WHERE username = ?"; // <- aggiunto rol
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next())
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null); // <- aggiunto null role

                int userId = rs.getInt("id_user");
                String passFromDb = rs.getString("pass");
                String role = rs.getString("rol"); // <- letto il ruolo

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
                return new LoginResult(LoginStatus.SUCCESS, userId, role); // <- ritorna anche il ruolo
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il check login", e);
            return new LoginResult(LoginStatus.ERROR, null, null); // <- aggiunto null role
        }
    }


    public boolean registerUser(String username, String password, String role, String email, String phone) {
        String sql = "INSERT INTO users (username, pass, rol, email, phone) VALUES (?, ?, ?, ?, ?)";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                String hashedPwd = BCrypt.hashpw(password, BCrypt.gensalt(12));
                stmt.setString(1, username);
                stmt.setString(2, hashedPwd);
                stmt.setString(3, role);
                stmt.setString(4, email);
                stmt.setString(5, phone);

                int rowsInserted = stmt.executeUpdate();
                return rowsInserted > 0;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante la registrazione utente", e);
            return false;
        }
    }

    public boolean isUsernameTaken(String username) {
        String sql = "SELECT username FROM users WHERE username = ?";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo username esistente", e);
            return false;
        }
    }

    public boolean isEmailTaken(String email) {
        String sql = "SELECT email FROM users WHERE email = ?";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo email esistente", e);
            return false;
        }
    }

    public static Integer findUserIdByUsername(String username) throws SQLException {
        final String sql = "SELECT id_user FROM users WHERE username = ?";
        Connection conn = DatabaseConnection.getInstance(); // NON chiudere
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public static void addInWishList(String username, long productId, int idShop, String pSize) throws SQLException {
        String sql = "INSERT IGNORE INTO wishlist(username, product_id, id_shop, p_size) VALUES(?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE p_size = VALUES(p_size)";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong   (2, productId);
            ps.setInt   (3, idShop);
            ps.setString(4, pSize);

            ps.executeUpdate();
        }
    }


    // Rimuove l’elemento con user+product+shop
    public static void removeInWishlist(String username, long productId, int idShop, String pSize) throws SQLException {
        String sql = "DELETE FROM wishlist WHERE username = ? AND product_id = ? AND id_shop = ? AND p_size = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, productId);
            ps.setInt(3, idShop);
            ps.setString(4, pSize);
            ps.executeUpdate();
        }
    }

    public static void clearWishlist(String username) throws SQLException {
        String sql = "DELETE FROM wishlist WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    public static List<Product> getFavorites(String username) throws SQLException {
        String sql =
                "SELECT p.product_id, p.name_p, p.sport, p.brand, p.category, " +
                        "       w.id_shop, w.p_size, s.name_s, pa.price, p.image_data " +
                        "FROM wishlist w " +
                        "JOIN products p ON p.product_id = w.product_id " +
                        "JOIN shops s    ON s.id_shop = w.id_shop " +
                        "LEFT JOIN product_availability pa " +
                        "  ON pa.product_id = w.product_id " +
                        " AND pa.id_shop    = w.id_shop " +
                        " AND pa.size       = w.p_size " +
                        "WHERE w.username = ?";

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


    private static Product mapRowToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setName(rs.getString("name_p"));
        p.setSport( rs.getString("sport"));
        p.setBrand( rs.getString("brand"));
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
