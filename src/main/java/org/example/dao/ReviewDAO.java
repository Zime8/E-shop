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

    // Risale a uno username in demo a partire da userId
    private static String resolveUsernameDemo(int userId) {
        if (userId < 0 && Session.getUser() != null) return Session.getUser();

        return DemoData.users().entrySet().stream()
                .filter(e -> e.getValue().id() != null && e.getValue().id() == userId)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> Session.getUser() != null ? Session.getUser() : ("user#" + userId));
    }

    // Restituisce le recensioni per (product, shop)
    public static List<Review> listByProductShop(long productId, int idShop) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            List<Review> src = DemoData.reviews().getOrDefault(DemoData.reviewKey(productId, idShop), Collections.emptyList());
            List<Review> copy = new ArrayList<>(src);
            copy.sort((a, b) -> {
                LocalDateTime ca = a.getCreatedAt();
                LocalDateTime cb = b.getCreatedAt();
                if (ca == null && cb == null) return 0;
                if (ca == null) return 1;
                if (cb == null) return -1;
                return cb.compareTo(ca);
            });
            return copy;
        }

        String call = "{ call sp_list_reviews(?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setLong(1, productId);
            cs.setInt(2, idShop);
            try (ResultSet rs = cs.executeQuery()) {
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

    // Inserisce/aggiorna la recensione dell’utente per (product, shop)
    public static void upsertReview(
            long productId,
            int idShop,
            int userId,
            int rating,
            String title,
            String comment
    ) throws SQLException {

        final String cleanTitle   = (title   == null || title.isBlank())   ? null : title.trim();
        final String cleanComment = (comment == null || comment.isBlank()) ? null : comment.trim();

        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            List<Review> list = DemoData.reviewsOf(productId, idShop);

            // rimuovi eventuale review dello stesso utente
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

        String call = "{ call sp_upsert_review(?, ?, ?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setLong(1, productId);
            cs.setInt(2, idShop);
            cs.setInt(3, userId);
            cs.setInt(4, rating);
            if (cleanTitle == null) cs.setNull(5, Types.VARCHAR); else cs.setString(5, cleanTitle);
            if (cleanComment == null) cs.setNull(6, Types.VARCHAR); else cs.setString(6, cleanComment);
            cs.executeUpdate();
        }
    }
}
