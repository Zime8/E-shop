package org.example.dao;

import org.example.models.entity.Review;

import java.util.List;

public interface ReviewRepository {
    List<Review> listByProductShop(long productId, int shopId);

    void upsertReview(
            long productId,
            int shopId,
            int userId,
            int rating,
            String title,
            String comment
    );
}