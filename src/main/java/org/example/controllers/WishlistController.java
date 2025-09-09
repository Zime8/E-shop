package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.dao.UserDAO;
import org.example.models.Product;
import org.example.util.Session;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WishlistController {

    @FXML private VBox itemsBox;
    @FXML private Label emptyLabel;
    @FXML private ScrollPane wishlistScroll;

    private Runnable onCartUpdated; // <- aggiungi
    public void setOnCartUpdated(Runnable r) { this.onCartUpdated = r; }

    private static final Logger logger = Logger.getLogger(WishlistController.class.getName());

    private static final double MAX_SCROLL_HEIGHT = 360;

    @FXML
    public void initialize() {
        itemsBox.setFillWidth(true);

        itemsBox.heightProperty().addListener((obs, oldH, newH) -> {
            double target = Math.min(newH.doubleValue() + 8, MAX_SCROLL_HEIGHT);
            wishlistScroll.setPrefHeight(target);
            wishlistScroll.setMaxHeight(MAX_SCROLL_HEIGHT);
        });

        wishlistScroll.setFitToWidth(true);
        wishlistScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        wishlistScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        loadItems();
    }

    public void loadItems() {
        itemsBox.getChildren().clear();
        String currentUser = Session.getUser();
        List<Product> list;

        try {
            list = UserDAO.getFavorites(currentUser);
        } catch (SQLException e) {
            list = List.of(); // vuota
        }

        if (list.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            for (Product p : list) {
                itemsBox.getChildren().add(createRow(p));
            }
        }
    }

    private HBox createRow(Product p) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        ImageView image = new ImageView();
        image.setFitWidth(50);
        image.setFitHeight(50);
        image.setPreserveRatio(true);

        StackPane thumb = new StackPane(image);
        thumb.setPrefSize(50, 50);
        thumb.setMinSize(50, 50);
        thumb.setMaxSize(50, 50);
        StackPane.setAlignment(image, Pos.CENTER);

        try {
            image.setImage(p.getImage());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore caricamento immagine prodotto: {0}", p.getName());
            logger.log(Level.WARNING, "Exception: ", e);
        }

        Label name = new Label(p.getName());
        name.setWrapText(true);
        name.setStyle("-fx-font-weight: bold");
        name.setAlignment(Pos.CENTER);
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);

        Label size = new Label("Taglia: " + (p.getSize() != null ? p.getSize() : "-"));
        size.setWrapText(true);
        size.setStyle("-fx-font-weight: bold");
        size.setAlignment(Pos.CENTER);
        HBox.setHgrow(size, Priority.ALWAYS);
        size.setMaxWidth(Double.MAX_VALUE);

        Label price = new Label(String.format("%.2f", p.getPrice()) + " €");
        price.setStyle("-fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        price.setAlignment(Pos.CENTER);

        Button btnRemove = new Button();
        btnRemove.setPrefSize(24, 24);
        btnRemove.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/basket.png")), 16,16,true,true)));
        btnRemove.setOnAction(e -> {
            try {
                UserDAO.removeInWishlist(Session.getUser(), p.getProductId(), p.getIdShop(), p.getSize());
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, String.format("Errore rimuovendo dalla wishlist (user=%s, productId=%d, shopId=%d, size=%s)",
                                Session.getUser(), p.getProductId(), p.getIdShop(), p.getSize()), ex);
                showAlert("Errore nella rimozione del prodotto dalla wishlist");
                return;
            }
            loadItems();
        });

        Button btnAddCart = new Button();
        btnAddCart.setPrefSize(24, 24);
        btnAddCart.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/cart.png")), 16,16,true,true)));
        btnAddCart.setOnAction(e -> {
            Session.addToCart(Product.copyOf(p));
            if (onCartUpdated != null) onCartUpdated.run();
        });

        row.getChildren().addAll(thumb, name, size, price, btnRemove, btnAddCart);
        return row;
    }

    @FXML
    private void onClearWishlist() {
        try {
            UserDAO.clearWishlist(Session.getUser());
            loadItems();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore nello svuotamento della wishlist", e);
            showAlert("Errore nello svuotamento della wishlist");
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
