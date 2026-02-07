package org.example.controllers.ui;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.example.controllers.app.WishlistAppController;
import org.example.models.Product;

import java.util.List;
import java.util.Objects;

public class WishlistController {

    @FXML private VBox itemsBox;
    @FXML private Label emptyLabel;
    @FXML private ScrollPane wishlistScroll;

    private WishlistAppController appController;
    private Runnable onCartUpdated;

    private static final double MAX_SCROLL_HEIGHT = 360;

    public void setOnCartUpdated(Runnable r) {
        this.onCartUpdated = r;
    }

    public void setAppController(WishlistAppController app) {
        this.appController = app;
        app.init(this::showItems, this::showAlert, this::notifyCartUpdated);
        app.loadItems();
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
        appController.clearWishlist();
    }

    public void showItems(List<Product> products) {
        itemsBox.getChildren().clear();
        if (products.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            for (Product p : products) {
                itemsBox.getChildren().add(createRow(p));
            }
        }
    }

    public void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void notifyCartUpdated(Product product) {
        if (onCartUpdated != null) {
            onCartUpdated.run();
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

        image.setImage(appController.loadProductImage(p));

        Label name = new Label(p.getName());
        name.setWrapText(true);
        name.setStyle("-fx-font-weight: bold");
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);

        Label size = new Label("Taglia: " + (p.getSize() != null ? p.getSize() : "-"));
        size.setWrapText(true);
        size.setStyle("-fx-font-weight: bold");
        HBox.setHgrow(size, Priority.ALWAYS);
        size.setMaxWidth(Double.MAX_VALUE);

        Label price = new Label(String.format("%.2f", p.getPrice()) + " €");
        price.setStyle("-fx-font-weight: bold; -fx-text-fill: #d32f2f;");

        Button btnRemove = new Button();
        btnRemove.setPrefSize(24, 24);
        btnRemove.setGraphic(new ImageView(new Image((
                Objects.requireNonNull(getClass().getResourceAsStream("/icons/basket.png"))), 16, 16, true, true)));
        btnRemove.setOnAction(e -> appController.removeFromWishlist(p));

        Button btnAddCart = new Button();
        btnAddCart.setPrefSize(24, 24);
        btnAddCart.setGraphic(new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/icons/cart.png")), 16, 16, true, true)));
        btnAddCart.setOnAction(e -> appController.addToCart(p));

        row.getChildren().addAll(thumb, name, size, price, btnRemove, btnAddCart);
        return row;
    }
}
