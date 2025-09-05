package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.demo.DemoData;
import org.example.models.Review;
import org.example.util.Session;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class ReviewDAO {

    private ReviewDAO(){}

    // Risale a uno username “umano” in demo a partire da userId
    private static String resolveUsernameDemo(int userId) {
        // se stai usando guest-UUID con id -1, usa direttamente la sessione
        if (userId < 0 && Session.getUser() != null) return Session.getUser();

        // prova a derivare dal seed utenti
        return DemoData.USERS.entrySet().stream()
                .filter(e -> e.getValue().id() != null && e.getValue().id() == userId)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> Session.getUser() != null ? Session.getUser() : ("user#" + userId));
    }

    /** Restituisce le recensioni per (product, shop) ordinate per createdAt DESC. */
    public static List<Review> listByProductShop(long productId, int idShop) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            // copia ordinata per non esporre la lista interna
            List<Review> src = DemoData.REVIEWS.getOrDefault(DemoData.reviewKey(productId, idShop), Collections.emptyList());
            List<Review> copy = new ArrayList<>(src);
            copy.sort((a, b) -> {
                LocalDateTime ca = a.getCreatedAt();
                LocalDateTime cb = b.getCreatedAt();
                if (ca == null && cb == null) return 0;
                if (ca == null) return 1;      // nulls last
                if (cb == null) return -1;
                return cb.compareTo(ca);       // desc
            });
            return copy;
        }

        // In DB: leggi i campi espliciti inclusi username
        String sql = """
            SELECT r.id_user, u.username, r.rating, r.title, r.p_comment, r.created_at
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
                    Timestamp ts = rs.getTimestamp("created_at");
                    out.add(new Review(
                            rs.getInt("id_user"),
                            rs.getString("username"),
                            rs.getInt("rating"),
                            rs.getString("title"),
                            rs.getString("p_comment"),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
                return out;
            }
        }
    }

    /** Inserisce/aggiorna la recensione dell’utente per (product, shop). */
    public static void upsertReview(
            long productId,
            int idShop,
            int userId,
            int rating,          // 1..5
            String title,        // opzionale
            String comment       // opzionale
    ) throws SQLException {

        final String cleanTitle   = (title   == null || title.isBlank())   ? null : title.trim();
        final String cleanComment = (comment == null || comment.isBlank()) ? null : comment.trim();

        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            List<Review> list = DemoData.reviewsOf(productId, idShop);

            // upsert: rimuovi eventuale review dello stesso utente
            for (Iterator<Review> it = list.iterator(); it.hasNext();) {
                Review r = it.next();
                if (r.getUserId() == userId) { it.remove(); break; }
            }

            Review newReview = new Review(
                    userId,
                    resolveUsernameDemo(userId),
                    rating,
                    cleanTitle,
                    cleanComment,
                    LocalDateTime.now()
            );
            list.add(newReview);
            return;
        }

        final String sql = """
            INSERT INTO product_reviews (product_id, id_shop, id_user, rating, title, p_comment)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              rating     = VALUES(rating),
              title      = VALUES(title),
              p_comment  = VALUES(p_comment),
              created_at = CURRENT_TIMESTAMP
        """;

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setInt(2, idShop);
            ps.setInt(3, userId);
            ps.setInt(4, rating);
            ps.setString(5, cleanTitle);
            ps.setString(6, cleanComment);
            ps.executeUpdate();
        }
    }
}
