    package org.example.controllers.app;

    import javafx.scene.control.Label;
    import javafx.scene.control.ToggleButton;
    import org.example.dao.ReviewDAO;
    import org.example.dao.UserDAO;
    import org.example.models.Product;
    import org.example.models.Review;
    import org.example.util.Session;

    import java.util.function.Consumer;
    import java.util.logging.Logger;
    import java.util.logging.Level;

    public class ReviewDialogAppController {
        private ReviewDAO reviewDao;
        private UserDAO userDao;
        private int rating = 5;
        private Product product;
        private Consumer<Review> onReviewSaved;

        private static final Logger logger = Logger.getLogger(ReviewDialogAppController.class.getName());

        @SuppressWarnings("unused") // Navigator
        public ReviewDialogAppController() {
            // Costruttore vuoto
        }

        public ReviewDialogAppController(Product product) {
            this.product = product;
        }

        public void loadData(Object data) {
            if (data instanceof Product p) {
                this.product = p;
            }
        }

        public void setOnReviewSaved(Consumer<Review> callback) {
            this.onReviewSaved = callback;
        }

        public void setReviewDao(ReviewDAO dao) { this.reviewDao = dao; }
        public void setUserDao(UserDAO dao) { this.userDao = dao; }

        public void setProduct(Product product) {
            this.product = product;
        }

        public void setupStars(ToggleButton star1, ToggleButton star2, ToggleButton star3,
                               ToggleButton star4, ToggleButton star5, Label ratingLabel) {
            star1.setUserData(1);
            star2.setUserData(2);
            star3.setUserData(3);
            star4.setUserData(4);
            star5.setUserData(5);

            setupStar(star1);
            setupStar(star2);
            setupStar(star3);
            setupStar(star4);
            setupStar(star5);

            setRating(5, star1, star2, star3, star4, star5, ratingLabel);
        }

        public void init(Label productTitle) {
            String productName = this.product.getName();
            String shopName = this.product.getNameShop() != null ? this.product.getNameShop() : "";
            String displayText = productName + (shopName.isBlank() ? "" : " • " + shopName);
            productTitle.setText(displayText);
        }

        public void onStarSelected(ToggleButton src, ToggleButton star1, ToggleButton star2, ToggleButton star3,
                                   ToggleButton star4, ToggleButton star5, Label ratingLabel) {
            Object ud = src.getUserData();
            int r = (ud instanceof Integer i) ? i : 1;
            setRating(r, star1, star2, star3, star4, star5, ratingLabel);
        }

        public void onCancel(Runnable closeAction) {
            closeAction.run();
        }

        public void onSave(String titleText, String commentText, Runnable close, Consumer<String> warn) {
            if (titleText.trim().isBlank()) {
                warn.accept("Titolo obbligatorio!");
                return;
            }
            if (commentText.trim().isBlank()) {
                warn.accept("Commento obbligatorio!");
                return;
            }

            Integer userId = findCurrentUserId();
            if (userId == null) {
                warn.accept("Effettua login per recensire!");
                return;
            }

            if (reviewDao == null) {
                warn.accept("DAO non inizializzato!");
                logger.warning("ReviewDAO è null - Navigator non ha iniettato");
                return;
            }

            try {
                reviewDao.upsertReview(
                        product.getProductId(),
                        product.getIdShop(),
                        userId,
                        rating,
                        titleText.trim(),
                        commentText.trim()
                );
                close.run();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Errore durante il salvataggio della recensione", e);
                warn.accept("Errore nel salvataggio della recensione.");
            }

            if (onReviewSaved != null) {
                onReviewSaved.accept(new Review(
                        findCurrentUserId(),
                        Session.getUser(),
                        rating,
                        titleText.trim(),
                        commentText.trim(),
                        java.time.LocalDateTime.now()
                ));
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
                logger.log(Level.WARNING, "Errore userId", e);
                return null;
            }
        }

        private void setRating(int r, ToggleButton s1, ToggleButton s2, ToggleButton s3,
                               ToggleButton s4, ToggleButton s5, Label ratingLabel) {
            this.rating = clamp(r);

            s1.setSelected(rating >= 1);
            s2.setSelected(rating >= 2);
            s3.setSelected(rating >= 3);
            s4.setSelected(rating >= 4);
            s5.setSelected(rating >= 5);

            ratingLabel.setText(rating + "/5");
        }

        private void setupStar(ToggleButton tb) {
            if (!tb.getStyleClass().contains("star")) tb.getStyleClass().add("star");
            updateStarClass(tb, tb.isSelected());
            tb.selectedProperty().addListener((obs, was, isSel) -> updateStarClass(tb, isSel));
        }

        private void updateStarClass(ToggleButton tb, boolean on) {
            if (on) {
                if (!tb.getStyleClass().contains("on")) tb.getStyleClass().add("on");
            } else {
                tb.getStyleClass().remove("on");
            }
        }

        private static int clamp(int v) {
            return (v < 1) ? 1 : Math.min(v, 5);
        }
    }
