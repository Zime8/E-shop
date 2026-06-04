package org.example.dao.db;

import org.example.dao.WishlistRepository;
import org.example.database.DatabaseConnection;
import org.example.models.entity.Product;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbWishlistDAO implements WishlistRepository {

    private static final Logger logger = Logger.getLogger(DbWishlistDAO.class.getName());

    @Override
    public void addInWishlist(String username, long productId, int idShop, String pSize) {

        requireNonBlank(username);

        try {
            final String call = "{ call sp_wishlist_add(?, ?, ?, ?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, username);
                cs.setLong(2, productId);
                cs.setInt(3, idShop);
                cs.setString(4, pSize);
                cs.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore durante addInWishlist", e);
        }
    }

    @Override
    public void removeInWishlist(String username, long productId, int idShop, String pSize) {

        requireNonBlank(username);

        try {
            final String call = "{ call sp_wishlist_remove(?, ?, ?, ?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, username);
                cs.setLong(2, productId);
                cs.setInt(3, idShop);
                cs.setString(4, pSize);
                cs.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore durante removeInWishlist", e);
        }
    }

    @Override
    public void clearWishlist(String username) {

        requireNonBlank(username);

        try {
            final String call = "{ call sp_wishlist_clear(?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, username);
                cs.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore durante clearWishlist", e);
        }
    }

    @Override
    public List<Product> getFavorites(String username) {

        requireNonBlank(username);

        try {
            final String call = "{ call sp_wishlist_get(?) }";
            try (Connection conn = DatabaseConnection.getConnection();
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
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore durante getFavorites", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void renameWishlistOwner(String currentUsername, String newUsername) {

        requireNonBlank(currentUsername);
        requireNonBlank(newUsername);

        if (currentUsername.equals(newUsername)) {
            return;
        }

        try {
            final String call = "{ call sp_wishlist_rename_owner(?, ?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, currentUsername);
                cs.setString(2, newUsername);
                cs.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore durante renameWishlistOwner", e);
        }
    }

    private void requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username non valido");
        }
    }

    private Product mapRowToProduct(ResultSet rs) throws SQLException {
        var createdAtTs = rs.getTimestamp("created_at");
        return new Product(
                rs.getLong("product_id"),
                rs.getInt("id_shop"),
                rs.getString("name_p"),
                rs.getString("sport"),
                rs.getString("brand"),
                rs.getString("category"),
                rs.getString("shop_name"),
                rs.getBigDecimal("price"),
                rs.getString("size"),
                rs.getBytes("image_data"),
                createdAtTs != null ? createdAtTs.toLocalDateTime() : null
        );
    }
}
