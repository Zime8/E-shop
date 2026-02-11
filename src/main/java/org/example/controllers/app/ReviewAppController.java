package org.example.controllers.app;

import org.example.dao.ReviewDAO;
import org.example.dao.UserDAO;
import org.example.models.Product;
import org.example.models.Review;
import org.example.util.Navigator;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
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

    public void openReviewDialog(Product product, Consumer<Optional<Review>> callback) {
        try {
            ReviewDialogAppController dialogAppController = new ReviewDialogAppController(product);
            dialogAppController.setReviewDao(reviewDao);
            dialogAppController.setUserDao(userDao);

            dialogAppController.setOnReviewSaved(review -> callback.accept(Optional.ofNullable(review)));

            Navigator.openModal(
                    "/fxml/ReviewDialog.fxml",
                    product,
                    dialogAppController,
                    () -> callback.accept(Optional.empty())
            );

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore apertura finestra recensione", e);
            callback.accept(Optional.empty());
        }
    }
}
