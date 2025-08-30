package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.models.CartItem;
import org.example.models.Product;
import org.example.util.Session;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CartController {

    @FXML private VBox cartItemsContainer;
    @FXML private Label totalLabel;
    @FXML private Label emptyCartLabel;
    @FXML private ScrollPane cartScroll;
    private Runnable onCartUpdated;

    private static final Logger logger = Logger.getLogger(CartController.class.getName());

    public void setOnCartUpdated(Runnable callback) { this.onCartUpdated = callback; }


    public void initialize() { loadCartItems(); }

    // ==== Helper per raggruppare per prodotto+shop+taglia ====
    private record Key(long productId, int shopId, String size) { }

    private static class Aggregated {
        final Product sample;  // un rappresentante per la riga (contiene immagine, nome, prezzo, ecc.)
        int qty;
        Aggregated(Product sample, int qty) { this.sample = sample; this.qty = qty; }
        double unitPrice() { return sample.getPrice(); }
        double subtotal() { return unitPrice() * qty; }
    }

    public void loadCartItems() {
        List<Product> cartItems = Session.getCartItems();

        cartItemsContainer.getChildren().clear();
        boolean hasItems = cartItems != null && !cartItems.isEmpty();
        toggleCartPlaceholders(hasItems);

        double total = 0.0;
        if (!hasItems) {
            updateTotalLabel(total);
            return;
        }

        // Riusa l’aggregazione già estratta per onCheckout()
        Map<Key, Aggregated> aggregated = aggregateCartItems(cartItems);

        for (Aggregated agg : aggregated.values()) {
            GridPane row = buildCartRow(agg);
            cartItemsContainer.getChildren().add(row);
            total += agg.subtotal();
        }

        updateTotalLabel(total);
    }

    private void toggleCartPlaceholders(boolean hasItems) {
        if (emptyCartLabel != null) {
            emptyCartLabel.setVisible(!hasItems);
            emptyCartLabel.setManaged(!hasItems);
        }
        if (cartScroll != null) {
            cartScroll.setVisible(hasItems);
            cartScroll.setManaged(hasItems);
        }
    }

    private void updateTotalLabel(double total) {
        if (totalLabel != null) {
            totalLabel.setText("€ " + String.format("%.2f", total));
        }
    }

    private GridPane buildCartRow(Aggregated agg) {
        Product p = agg.sample;

        GridPane row = new GridPane();
        row.setMinHeight(56);
        row.setAlignment(Pos.CENTER);
        row.setStyle("-fx-background-color: transparent;"
                + "-fx-border-color: #d32f2f;"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: 8;"
                + "-fx-padding: 6;");

        configureRowGrid(row);

        ImageView imageView = createProductImage(p);
        Label name = createNameLabel(p);
        Label unitPrice = createUnitPriceLabel(agg);
        HBox qtyBox = createQtyBox(p, agg);
        Label sub = createSubtotalLabel(agg);
        Button removeAll = createRemoveAllButton(p, agg);

        row.add(imageView, 0, 0);
        row.add(name,      1, 0);
        row.add(unitPrice, 2, 0);
        row.add(qtyBox,    3, 0);
        row.add(sub,       4, 0);
        row.add(removeAll, 5, 0);

        return row;
    }

    private void configureRowGrid(GridPane row) {
        ColumnConstraints cImg = new ColumnConstraints();  cImg.setPercentWidth(12); cImg.setHalignment(HPos.CENTER);
        ColumnConstraints cName = new ColumnConstraints(); cName.setPercentWidth(33); cName.setHalignment(HPos.LEFT);
        ColumnConstraints cUnit = new ColumnConstraints(); cUnit.setPercentWidth(13); cUnit.setHalignment(HPos.CENTER);
        ColumnConstraints cQty = new ColumnConstraints();  cQty.setPercentWidth(15); cQty.setHalignment(HPos.CENTER);
        ColumnConstraints cSub = new ColumnConstraints();  cSub.setPercentWidth(17); cSub.setHalignment(HPos.CENTER);
        ColumnConstraints cRem = new ColumnConstraints();  cRem.setPercentWidth(10); cRem.setHalignment(HPos.CENTER);
        row.getColumnConstraints().addAll(cImg, cName, cUnit, cQty, cSub, cRem);
    }

    private ImageView createProductImage(Product p) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        imageView.setPreserveRatio(true);
        try {
            imageView.setImage(p.getImage());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore caricamento immagine: {0}", p.getName());
            logger.log(Level.WARNING, "Exception: ", e);
        }
        return imageView;
    }

    private Label createNameLabel(Product p) {
        String sizeText = (p.getSize() != null) ? "  (Taglia: " + p.getSize() + ")" : "";
        Label name = new Label(p.getName() + sizeText);
        name.setStyle("-fx-font-weight: bold;");
        return name;
    }

    private Label createUnitPriceLabel(Aggregated agg) {
        Label unitPrice = new Label(String.format("€ %.2f", agg.unitPrice()));
        unitPrice.setStyle("-fx-text-fill: #d32f2f;");
        return unitPrice;
    }

    private HBox createQtyBox(Product p, Aggregated agg) {
        HBox qtyBox = new HBox(8);
        qtyBox.setAlignment(Pos.CENTER);

        Button minus = new Button("-");
        Label qtyLbl = new Label(String.valueOf(agg.qty));
        Button plus = new Button("+");

        minus.setOnAction(e -> {
            Session.removeFromCart(p);
            loadCartItems();
            if (onCartUpdated != null) onCartUpdated.run();
        });

        plus.setOnAction(e -> {
            Session.addToCart(Product.copyOf(p));
            loadCartItems();
            if (onCartUpdated != null) onCartUpdated.run();
        });

        qtyBox.getChildren().addAll(minus, qtyLbl, plus);
        return qtyBox;
    }

    private Label createSubtotalLabel(Aggregated agg) {
        Label sub = new Label(String.format("€ %.2f", agg.subtotal()));
        sub.setStyle("-fx-font-weight: bold;");
        return sub;
    }

    private Button createRemoveAllButton(Product p, Aggregated agg) {
        Button removeAll = new Button();
        removeAll.setPrefSize(24, 24);
        removeAll.setGraphic(new ImageView(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/basket.png")),
                        16, 16, true, true)));

        removeAll.setOnAction(e -> {
            for (int i = 0; i < agg.qty; i++) {
                Session.removeFromCart(p);
            }
            loadCartItems();
            if (onCartUpdated != null) onCartUpdated.run();
        });
        return removeAll;
    }


    @FXML
    private void onCheckout() {
        List<Product> products = Session.getCartItems();
        if (products == null || products.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Il carrello è vuoto.").showAndWait();
            return;
        }

        Map<Key, Aggregated> aggregated = aggregateCartItems(products);
        CheckoutData data = buildCheckoutData(aggregated);
        openOrderSummary(data);
    }

    // Raggruppa gli articoli per (productId, shopId, taglia)
    private Map<Key, Aggregated> aggregateCartItems(List<Product> products) {
        Map<Key, Aggregated> map = new LinkedHashMap<>();
        for (Product p : products) {
            Key k = new Key(p.getProductId(), p.getIdShop(), p.getSize());
            map.compute(k, (key, agg) -> {
                if (agg == null) return new Aggregated(p, 1);
                agg.qty += 1;
                return agg;
            });
        }
        return map;
    }

    // Converte gli aggregati in CartItem e calcola il totale
    private CheckoutData buildCheckoutData(Map<Key, Aggregated> map) {
        List<CartItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Aggregated agg : map.values()) {
            Product p = agg.sample;
            int qty = agg.qty;
            double priceDouble = p.getPrice();

            items.add(new CartItem(
                    p.getProductId(),
                    p.getIdShop(),
                    qty,
                    priceDouble,
                    p.getName(),
                    p.getImage(),
                    p.getSize()
            ));

            BigDecimal unit = BigDecimal.valueOf(priceDouble);
            total = total.add(unit.multiply(BigDecimal.valueOf(qty)));
        }
        return new CheckoutData(items, total);
    }

    // Trova la finestra "owner" corretta per la dialog
    private Window resolveOwnerWindow() {
        Window owner = null;

        if (cartItemsContainer != null && cartItemsContainer.getScene() != null) {
            owner = cartItemsContainer.getScene().getWindow();
        }

        if (owner instanceof Popup popup) {
            owner = popup.getOwnerWindow();
        }

        if (owner == null) {
            for (Window w : Window.getWindows()) {
                if (w instanceof Stage && w.isShowing()) {
                    owner = w;
                    break;
                }
            }
        }
        return owner;
    }

    // Apre il riepilogo ordine e ricarica il carrello al termine
    private void openOrderSummary(CheckoutData data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OrderSummary.fxml"));
            Parent root = loader.load();
            OrderSummaryController ctrl = loader.getController();

            ctrl.setOnCartUpdated(this.onCartUpdated);

            Stage dialog = new Stage();
            Window owner = resolveOwnerWindow();

            if (owner instanceof Stage stage && stage.isShowing()) {
                dialog.initOwner(stage);
                dialog.initModality(Modality.WINDOW_MODAL);
            } else {
                dialog.initModality(Modality.APPLICATION_MODAL);
            }

            dialog.setScene(new Scene(root));
            ctrl.setStage(dialog);
            ctrl.setData(data.items(), data.total());

            dialog.showAndWait();
            loadCartItems();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossibile aprire il riepilogo ordine", e);
            new Alert(Alert.AlertType.ERROR,
                    "Impossibile aprire il riepilogo ordine: " + e.getMessage()).showAndWait();
        }
    }

    // Piccolo contenitore per i dati del checkout
    private record CheckoutData(List<CartItem> items, BigDecimal total) {}

    @FXML
    private void onClearCart() {
        Session.clearCart();
        loadCartItems();
        if (onCartUpdated != null) onCartUpdated.run();
    }

}
