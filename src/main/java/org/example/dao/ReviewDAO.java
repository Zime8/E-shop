package org.example.dao;

import org.example.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public record Review(int rating, String title, String comment, String username, java.sql.Timestamp createdAt) {}

    public static List<Review> listByProductShop(long productId, int idShop) throws SQLException {
        String sql = """
            SELECT r.*
            FROM product_reviews r
            JOIN users u ON u.id_user = r.id_user
            WHERE r.product_id = ? AND r.id_shop = ?
            ORDER BY r.created_at DESC
        """;
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setInt(2, idShop);
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Review(
                            rs.getInt("rating"),
                            rs.getString("title"),
                            rs.getString("p_comment"),
                            rs.getString("id_user"),
                            rs.getTimestamp("created_at")
                    ));
                }
                return out;
            }
        }
    }

    // Inserisce o aggiorna una recensione per (product_id, id_shop, id_user).
    public static void upsertReview(
            long productId,
            int idShop,
            int userId,
            int rating,          // 1..5
            String title,        // opzionale
            String comment       // opzionale
    ) throws SQLException {

        final String sql = """
            INSERT INTO product_reviews (product_id, id_shop, id_user, rating, title, p_comment)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              rating     = VALUES(rating),
              title      = VALUES(title),
              p_comment  = VALUES(p_comment),
              created_at = CURRENT_TIMESTAMP
            """;

        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setInt(2, idShop);
            ps.setInt(3, userId);
            ps.setInt(4, rating);
            ps.setString(5, title == null || title.isBlank() ? null : title.trim());
            ps.setString(6, comment == null || comment.isBlank() ? null : comment.trim());
            ps.executeUpdate();
        }
    }
}
