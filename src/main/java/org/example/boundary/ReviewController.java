package org.example.boundary;

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
import org.example.control.ReviewAppController;
import org.example.models.dto.ProductDto;
import org.example.models.dto.ReviewDto;
import org.example.util.Navigator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReviewController {

    @FXML private Label productTitle;
    @FXML private Label avgLabel;
    @FXML private Label countLabel;
    @FXML private VBox reviewsBox;
    @FXML private Button closeBtn;
    @FXML private Button addBtn;
    @FXML private ProgressIndicator progress;

    private ReviewAppController appController;
    private Navigator navigator;
    private ProductDto product;

    public void setAppController(ReviewAppController app) {
        this.appController = app;
    }

    public void setNavigator(Navigator navigator){
        this.navigator = navigator;
    }

    @SuppressWarnings("unused")
    public void loadData(Object dataObj) {
        if (dataObj instanceof ProductDto p) {
            setProduct(p);
        }
    }

    public void setProduct(ProductDto product) {
        this.product = product;
        String shop = (product.nameShop() != null && !product.nameShop().isBlank())
                ? " • " + product.nameShop() : "";
        productTitle.setText(product.name() + shop);
        loadReviews();
    }

    private void loadReviews() {

        if (appController == null || product == null) {
            progress.setVisible(false);
            return;
        }

        reviewsBox.getChildren().clear();
        progress.setVisible(true);

        List<ReviewDto> list = appController.loadReviews(product);

        double avg = list.stream().mapToInt(ReviewDto::rating).average().orElse(0.0);
        int count = list.size();
        avgLabel.setText(String.format("Voto medio: %.1f/5", avg));
        countLabel.setText("(" + count + " recensioni)");

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (ReviewDto r : list) {
            reviewsBox.getChildren().add(buildRow(r, df));
        }

        if (list.isEmpty()) {
            Label empty = new Label("Ancora nessuna recensione. Sii il primo a scriverne una!");
            empty.setStyle("-fx-text-fill:#666;");
            reviewsBox.getChildren().add(empty);
        }

        progress.setVisible(false);

    }

    private HBox buildRow(ReviewDto r, DateTimeFormatter df) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: transparent;"
                + "-fx-border-color: #e5b4b4;"
                + "-fx-border-width: 1.2; -fx-border-radius: 10; -fx-background-radius: 10;");
        row.setFillHeight(true);

        int rating = r.rating();
        String stars = "★★★★★".substring(0, rating) + "☆☆☆☆☆".substring(0, 5 - rating);
        Label starsLbl = new Label(stars);
        starsLbl.setStyle("-fx-text-fill:#d32f2f; -fx-font-size:14; -fx-font-weight:bold;");

        Label title = new Label(r.title() != null ? r.title() : "");
        title.setStyle("-fx-font-weight:bold; -fx-font-size:13;");

        LocalDateTime created = r.createdAt();
        String metaTxt = (r.username() != null ? r.username() : "utente")
                + (created != null ? " • " + df.format(created) : "");
        Label meta = new Label(metaTxt);
        meta.setStyle("-fx-text-fill:#777; -fx-font-size:11;");

        Label comment = new Label(r.comment() != null ? r.comment() : "");
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
        if (appController == null || product == null) return;

        if (!appController.canCurrentUserReview()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Devi effettuare il login per inserire una recensione");
            alert.initOwner(addBtn.getScene().getWindow());
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = navigator.createAppLoader("/fxml/ReviewDialog.fxml");
            Parent root = loader.load();

            ReviewDialogController controller = loader.getController();
            controller.setAppController(appController);
            controller.setProduct(product);
            controller.setOnSaved(savedReview -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Recensione salvata!");
                alert.initOwner(addBtn.getScene().getWindow());
                alert.showAndWait();
                loadReviews();
            });
            controller.setOnCancelled(() -> {});

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Scrivi recensione");
            dialogStage.initOwner(addBtn.getScene().getWindow());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Errore apertura recensione: " + e.getMessage());
            alert.initOwner(addBtn.getScene().getWindow());
            alert.showAndWait();
        }
    }
}
