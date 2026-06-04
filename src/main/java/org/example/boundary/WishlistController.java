package org.example.boundary;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.example.control.WishlistAppController;
import org.example.models.dto.WishlistItemDto;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WishlistController {

    @FXML private VBox itemsBox;
    @FXML private Label emptyLabel;
    @FXML private ScrollPane wishlistScroll;

    private WishlistAppController appController;
    private Runnable onCartUpdated;

    private static final Logger logger = Logger.getLogger(WishlistController.class.getName());

    private static final double MAX_SCROLL_HEIGHT = 360;

    public void setOnCartUpdated(Runnable r) {
        this.onCartUpdated = r;
    }

    public void setAppController(WishlistAppController app) {
        this.appController = app;
        refreshWishlist();
    }

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
    }

    @FXML
    private void onClearWishlist() {
        if (appController != null) {
            try {
                appController.clearWishlist();
                refreshWishlist();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore nello svuotamento wishlist", e);
                showAlert("Errore nello svuotamento della wishlist");
            }
        }
    }

    private void refreshWishlist() {
        if (appController == null) {
            showItems(List.of());
            return;
        }

        try {
            List<WishlistItemDto> items = appController.loadItems();
            showItems(items);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento wishlist", e);
            showItems(List.of());
        }
    }

    public void showItems(List<WishlistItemDto> products) {
        itemsBox.getChildren().clear();
        if (products.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            for (WishlistItemDto p : products) {
                itemsBox.getChildren().add(createRow(p));
            }
        }
    }

    public void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void notifyCartUpdated() {
        if (onCartUpdated != null) {
            onCartUpdated.run();
        }
    }

    private HBox createRow(WishlistItemDto p) {
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

        image.setImage(loadProductImage(p));

        Label name = new Label(p.name());
        name.setWrapText(true);
        name.setStyle("-fx-font-weight: bold");
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);

        Label size = new Label("Taglia: " + (p.size() != null ? p.size() : "-"));
        size.setWrapText(true);
        size.setStyle("-fx-font-weight: bold");
        HBox.setHgrow(size, Priority.ALWAYS);
        size.setMaxWidth(Double.MAX_VALUE);

        Label price = new Label(p.displayPrice());
        price.setStyle("-fx-font-weight: bold; -fx-text-fill: #d32f2f;");

        Button btnRemove = new Button();
        btnRemove.setPrefSize(24, 24);
        btnRemove.setGraphic(new ImageView(new Image((
                Objects.requireNonNull(getClass().getResourceAsStream("/icons/basket.png"))), 16, 16, true, true)));
        btnRemove.setOnAction(e -> {
            if (appController != null) {
                try {
                    appController.removeFromWishlist(p);
                    refreshWishlist();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Errore nella rimozione wishlist", ex);
                    showAlert("Errore nella rimozione del prodotto dalla wishlist");
                }
            }
        });

        Button btnAddCart = new Button();
        btnAddCart.setPrefSize(24, 24);
        btnAddCart.setGraphic(new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/icons/cart.png")), 16, 16, true, true)));
        btnAddCart.setOnAction(e -> {
            if (appController != null) {
                try {
                    appController.addToCart(p);
                    notifyCartUpdated();
                    Platform.runLater(this::refreshWishlist);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Errore aggiungendo al carrello", ex);
                    showAlert("Errore nell'aggiunta del prodotto al carrello");
                }
            }
        });

        row.getChildren().addAll(thumb, name, size, price, btnRemove, btnAddCart);
        return row;
    }

    public Image loadProductImage(WishlistItemDto p) {
        try {
            byte[] data = p.imageData();
            if (data == null || data.length == 0) {
                return null;
            }
            Image img = new Image(new java.io.ByteArrayInputStream(data));
            return img.isError() ? null : img;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore caricamento immagine", e);
            return null;
        }
    }
}
