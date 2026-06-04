package org.example.control;

import org.example.control.session.UserContext;
import org.example.dao.ReviewRepository;
import org.example.dao.UserRepository;
import org.example.models.dto.ProductDto;
import org.example.models.dto.ReviewDto;
import org.example.models.entity.Review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReviewAppController {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final UserContext userContext;
    private static final Logger logger = Logger.getLogger(ReviewAppController.class.getName());

    public ReviewAppController(ReviewRepository reviewRepository,
                               UserRepository userRepository,
                               UserContext userContext) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.userContext = userContext;
    }

    public List<ReviewDto> loadReviews(ProductDto product) {
        try {
            return reviewRepository.listByProductShop(product.productId(), product.shopId())
                    .stream().map(this::toDto).toList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore caricamento recensioni", e);
            return List.of();
        }
    }

    public boolean canCurrentUserReview() {
        String username = userContext.getCurrentUsername();
        return username != null && !username.isBlank();
    }

    public ReviewDto saveReview(ProductDto product, int rating, String titleText, String commentText) {

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Valutazione non valida");
        }
        if (titleText == null || titleText.trim().isBlank()) {
            throw new IllegalArgumentException("Titolo obbligatorio!");
        }
        if (commentText == null || commentText.trim().isBlank()) {
            throw new IllegalArgumentException("Commento obbligatorio!");
        }
        if (product == null) {
            throw new IllegalArgumentException("Prodotto non disponibile");
        }

        Integer userId = findCurrentUserId();
        String username = userContext.getCurrentUsername();

        if (userId == null || username == null || username.isBlank()) {
            throw new IllegalStateException("Effettua login per recensire!");
        }

        String cleanTitle = titleText.trim();
        String cleanComment = commentText.trim();

        try {
            reviewRepository.upsertReview(
                    product.productId(),
                    product.shopId(),
                    userId,
                    rating,
                    cleanTitle,
                    cleanComment
            );

            return new ReviewDto(
                    username,
                    rating,
                    cleanTitle,
                    cleanComment,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore durante il salvataggio della recensione", e);
            throw new IllegalStateException("Errore nel salvataggio della recensione.", e);
        }
    }

    private Integer findCurrentUserId() {
        Integer userId = userContext.getCurrentUserId();
        if (userId != null) return userId;

        String username = userContext.getCurrentUsername();
        if (username == null || username.isBlank()) return null;

        try {
            return userRepository.findUserIdByUsername(username);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore userId", e);
            return null;
        }
    }

    private ReviewDto toDto(Review r) {
        return new ReviewDto(
                r.getUsername(),
                r.getRating(),
                r.getTitle(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}