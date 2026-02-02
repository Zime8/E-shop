package org.example.controllers.app;

import org.example.dao.ReviewDAO;
import org.example.dao.UserDAO;
import org.example.models.Product;
import org.example.models.Review;
import org.example.util.Session;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReviewAppController {
    private final ReviewDAO reviewDao =  new ReviewDAO();
    private final UserDAO userDao = new UserDAO();
    private static final Logger logger = Logger.getLogger(ReviewAppController.class.getName());

    public List<Review> loadReviews(Product product) {
        try {
            return reviewDao.listByProductShop(product.getProductId(), product.getIdShop());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore caricamento recensioni");
            return List.of();
        }
    }

    public Integer findCurrentUserId() {
        Integer userId = Session.getUserId();
        if (userId != null) return userId;

        String username = Session.getUser();
        if (username == null || username.isBlank()) return null;

        try {
            return userDao.findUserIdByUsername(username);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore ricerca user");
            return null;
        }
    }

    public void upsertReview(long productId, int shopId, Integer userId,
                             int rating, String title, String comment) {
        try {
            reviewDao.upsertReview(productId, shopId, userId, rating, title, comment);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore upsertReview");
        }
    }
}
