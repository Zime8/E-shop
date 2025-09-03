package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.dao.ReviewDAO;
import org.example.dao.UserDAO;
import org.example.models.Product;
import org.example.models.Review;        // ⬅️ usa il model
import org.example.util.Session;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListReviewController {

    @FXML private Label productTitle;
    @FXML private Label avgLabel;
    @FXML private Label countLabel;
    @FXML private VBox reviewsBox;
    @FXML private Button closeBtn;
    @FXML private Button addBtn;
    @FXML private ProgressIndicator progress;

    private static final Logger logger = Logger.getLogger(ListReviewController.class.getName());
    private Product product;

    public void init(Product product) {
        this.product = product;
        String shop = (product.getNameShop() != null && !product.getNameShop().isBlank())
                ? " • " + product.getNameShop() : "";
        productTitle.setText(product.getName() + shop);
        loadReviews();
    }

    private void loadReviews() {
        reviewsBox.getChildren().clear();
        progress.setVisible(true);

        try {
            List<Review> list = ReviewDAO.listByProductShop(product.getProductId(), product.getIdShop());

            double avg = list.stream().mapToInt(Review::getRating).average().orElse(0.0);
            int count = list.size();
            avgLabel.setText(String.format("Voto medio: %.1f/5", avg));
            countLabel.setText("(" + count + " recensioni)");

            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (Review r : list) {
                reviewsBox.getChildren().add(buildRow(r, df));
            }

            if (list.isEmpty()) {
                Label empty = new Label("Ancora nessuna recensione. Sii il primo a scriverne una!");
                empty.setStyle("-fx-text-fill:#666;");
                reviewsBox.getChildren().add(empty);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore caricando recensioni", e);
            reviewsBox.getChildren().add(new Label("Impossibile caricare le recensioni."));
        } finally {
            progress.setVisible(false);
        }
    }

    private HBox buildRow(Review r, DateTimeFormatter df) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: transparent;"
                + "-fx-border-color: #e5b4b4;"
                + "-fx-border-width: 1.2; -fx-border-radius: 10; -fx-background-radius: 10;");
        row.setFillHeight(true);

        int rating = r.getRating();
        String stars = "★★★★★".substring(0, rating) + "☆☆☆☆☆".substring(0, 5 - rating);
        Label starsLbl = new Label(stars);
        starsLbl.setStyle("-fx-text-fill:#d32f2f; -fx-font-size:14; -fx-font-weight:bold;");

        Label title = new Label(r.getTitle() != null ? r.getTitle() : "");
        title.setStyle("-fx-font-weight:bold; -fx-font-size:13;");

        LocalDateTime created = r.getCreatedAt();
        String metaTxt = (r.getUsername() != null ? r.getUsername() : "utente")
                + (created != null ? " • " + df.format(created) : "");
        Label meta = new Label(metaTxt);
        meta.setStyle("-fx-text-fill:#777; -fx-font-size:11;");

        Label comment = new Label(r.getComment() != null ? r.getComment() : "");
        comment.setWrapText(true);

        VBox center = new VBox(2, starsLbl, title, meta, comment);
        HBox.setHgrow(center, Priority.ALWAYS);

        row.getChildren().addAll(center);
        return row;
    }

    @FXML
    private void onClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }

    @FXML
    private void onAdd() {
        try {
            String username = Session.getUser();
            if (username == null || username.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Effettua l'accesso per lasciare una recensione.").showAndWait();
                return;
            }

            // usa prima l'id già in sessione (funziona anche in demo: -1), poi il lookup DB come fallback
            Integer userId = Session.getUserId();
            if (userId == null) {
                userId = UserDAO.findUserIdByUsername(username);
            }
            if (userId == null && Session.isDemo()) {
                userId = -1; // id fittizio per l’ospite demo
            }
            if (userId == null) {
                new Alert(Alert.AlertType.ERROR, "Utente corrente non trovato.").showAndWait();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReviewDialog.fxml"));
            Parent root = loader.load();

            ReviewDialogController dialogCtrl = loader.getController();
            dialogCtrl.init(product);

            Stage st = new Stage();
            st.setTitle("Scrivi una recensione");
            st.initOwner(addBtn.getScene().getWindow());
            st.initModality(Modality.WINDOW_MODAL);
            st.setScene(new Scene(root));
            st.showAndWait();

            var res = dialogCtrl.getResult();
            if (res.isEmpty()) return;

            var data = res.get();
            ReviewDAO.upsertReview(
                    product.getProductId(),
                    product.getIdShop(),
                    userId,
                    data.rating(),
                    data.title(),
                    data.comment()
            );

            new Alert(Alert.AlertType.INFORMATION, "Grazie! La tua recensione è stata salvata.").showAndWait();
            loadReviews();

        } catch (Exception ex) {
            logger.log(Level.WARNING, "Errore durante inserimento recensione", ex);
            new Alert(Alert.AlertType.ERROR, "Impossibile salvare la recensione:\n" + ex.getMessage()).showAndWait();
        }
    }
}
