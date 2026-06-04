package org.example.dao.db;

import org.example.dao.ReviewRepository;
import org.example.database.DatabaseConnection;
import org.example.models.entity.Review;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DbReviewDAO implements ReviewRepository {

    private static final Logger logger = Logger.getLogger(DbReviewDAO.class.getName());

    // Restituisce le recensioni per (product, shop)
    @Override
    public List<Review> listByProductShop(long productId, int shopId) {
        try {
            String call = "{ call sp_list_reviews(?, ?) }";

            try (Connection connection = DatabaseConnection.getConnection();
                 CallableStatement cs = connection.prepareCall(call)) {
                cs.setLong(1, productId);
                cs.setInt(2, shopId);
                try (ResultSet resultSet = cs.executeQuery()) {
                    List<Review> reviews = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp ts = resultSet.getTimestamp("created_at");
                        reviews.add(new Review(
                                resultSet.getInt("id_user"),
                                resultSet.getString("username"),
                                resultSet.getInt("rating"),
                                resultSet.getString("title"),
                                resultSet.getString("p_comment"),
                                ts != null ? ts.toLocalDateTime() : null
                        ));
                    }
                    return reviews;
                }
            }
        } catch (SQLException e){
            logger.log(Level.WARNING, "Errore durante listByProductShop", e);
            return List.of();
        }
    }

    // Inserisce/aggiorna la recensione dell’utente per (product, shop)
    @Override
    public void upsertReview(
            long productId,
            int shopId,
            int userId,
            int rating,
            String title,
            String comment
    ) {

        try {
            String cleanTitle = normalize(title);
            String cleanComment = normalize(comment);

            String call = "{ call sp_upsert_review(?, ?, ?, ?, ?, ?) }";
            try (Connection connection = DatabaseConnection.getConnection();
                 CallableStatement statement = connection.prepareCall(call)) {
                statement.setLong(1, productId);
                statement.setInt(2, shopId);
                statement.setInt(3, userId);
                statement.setInt(4, rating);
                if (cleanTitle == null) statement.setNull(5, Types.VARCHAR);
                else statement.setString(5, cleanTitle);
                if (cleanComment == null) statement.setNull(6, Types.VARCHAR);
                else statement.setString(6, cleanComment);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore durante upsertReview", e);
        }
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
