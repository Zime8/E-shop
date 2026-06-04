package org.example.dao.demo;

import org.example.dao.ReviewRepository;
import org.example.demo.DemoData;
import org.example.models.entity.Review;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class DemoReviewDAO implements ReviewRepository {

    @Override
    public List<Review> listByProductShop(long productId, int shopId){
        DemoData.ensureLoaded();

        List<Review> src = DemoData.reviews().getOrDefault(DemoData.reviewKey(productId, shopId), Collections.emptyList());
        List<Review> reviews = new ArrayList<>(src);

        reviews.sort(Comparator.comparing(
                Review::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return reviews;
    }

    @Override
    public void upsertReview(
            long productId,
            int shopId,
            int userId,
            int rating,
            String title,
            String comment
    ){
        DemoData.ensureLoaded();

        String cleanTitle = normalize(title);
        String cleanComment = normalize(comment);

        List<Review> reviews = DemoData.reviewsOf(productId, shopId);

        // rimuovi eventuale review dello stesso utente
        for (Iterator<Review> it = reviews.iterator(); it.hasNext(); ) {
            Review r = it.next();
            if (r.getUserId() == userId) {
                it.remove();
                break;
            }
        }

        reviews.add(new Review(
                userId,
                resolveUsername(userId),
                rating,
                cleanTitle,
                cleanComment,
                LocalDateTime.now(Clock.system(ZoneId.of("Europe/Rome")))
        ));
    }

    private String resolveUsername(int userId) {

        return DemoData.users().entrySet().stream()
                .filter(e -> e.getValue().id() != null && e.getValue().id() == userId)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("user#" + userId);
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
