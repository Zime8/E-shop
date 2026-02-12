package org.example.controllers.ui;

import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.controllers.app.BuyProductController;
import org.example.models.*;
import org.example.util.Navigator;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CartController {

    private BuyProductController appController;

    @FXML private VBox cartItemsContainer;
    @FXML private Label totalLabel;
    @FXML private Label emptyCartLabel;
    @FXML private ScrollPane cartScroll;
    private Runnable onCartUpdated;

    private static final Logger logger = Logger.getLogger(CartController.class.getName());
    private static final String MSG_STOCK_UNKNOWN = "Disponibilità non verificabile";

    public void setOnCartUpdated(Runnable callback) { this.onCartUpdated = callback; }

    public void setAppController(BuyProductController app) {
        this.appController = app;
        loadCartItems();
    }

    // Carica i prodotti nel carrello
    public void loadCartItems() {
        cartItemsContainer.getChildren().clear();
        try {
            CheckoutData data = appController.buildCheckoutData();
            List<CartItem> cartItems = appController.getCartItems();
            boolean hasItems = !cartItems.isEmpty();
            toggleCartPlaceholders(hasItems);

            if (!hasItems) {
                updateTotalLabel(0.0);
                return;
            }

            double total = data.total().doubleValue();
            Map<Key, Aggregated> aggregated = appController.getAggregatedCart();

            for (Aggregated agg : aggregated.values()) {
                GridPane row = buildCartRow(agg);
                cartItemsContainer.getChildren().add(row);
            }
            updateTotalLabel(total);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento carrello", e);
            toggleCartPlaceholders(false);
            updateTotalLabel(0.0);
        }
    }

    @FXML private void onCheckout() {
        CheckoutData data = appController.buildCheckoutData();
        if (data.items().isEmpty()) return;

        Navigator.openModal("/fxml/OrderSummary.fxml", data, appController, this.onCartUpdated);
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
        HBox qtyBox = createQtyBox(agg);
        Label sub = createSubtotalLabel(agg);
        Button removeAll = createRemoveAllButton(p);

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
        Image img = toImage(p.getImageData());
        imageView.setImage(img);

        return imageView;
    }

    private static Image toImage(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return new Image(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Impossibile creare l'immagine dal byte[]", e);
            return null;
        }
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

    private HBox createQtyBox(Aggregated agg) {
        HBox qtyBox = new HBox(8);
        qtyBox.setAlignment(Pos.CENTER);

        Button minus = new Button("-");
        Label qtyLbl = new Label(String.valueOf(agg.getQty()));
        Button plus = new Button("+");

        CartItem item = new CartItem(
                agg.sample.getProductId(), agg.sample.getIdShop(), agg.getQty(),
                agg.unitPrice(), agg.sample.getName(), agg.sample.getImageData(), agg.sample.getSize()
        );

        try {
            int stock = appController.getStockFor(item.getProductId(), item.getShopId(), item.getSize());
            setupNormalQtyBox(minus, plus, stock, agg.getQty(), item);
        } catch (Exception ex) {
            // In caso di errore niente incremento
            logger.log(Level.WARNING, ex, () -> "Impossibile leggere lo stock per " + agg.sample.getName());
            setupErrorQtyBox(minus, plus, qtyLbl, item, agg);
        }

        qtyBox.getChildren().addAll(minus, qtyLbl, plus);
        return qtyBox;
    }

    private void setupNormalQtyBox(Button minus, Button plus, int stock, int currentQty,
                                   CartItem item) {
        if (currentQty >= stock) {
            plus.setDisable(true);
            stockLabelTooltip(plus, "Quantità massima raggiunta: " + stock);
        }

        minus.setOnAction(e -> {
            appController.changeQuantity(item, -1);
            refreshView();
        });
        plus.setOnAction(e -> {
            appController.changeQuantity(item, +1);
            refreshView();
        });
    }

    private void setupErrorQtyBox(Button minus, Button plus, Label qtyLbl, CartItem item, Aggregated agg) {
        plus.setDisable(true);
        stockLabelTooltip(plus, MSG_STOCK_UNKNOWN);
        stockLabelTooltip(qtyLbl, MSG_STOCK_UNKNOWN);
        stockLabelTooltip(minus, MSG_STOCK_UNKNOWN);
        minus.setOnAction(e -> {
            if (item != null) {
                appController.changeQuantity(item, -1);
            } else {
                appController.removeLine(agg.sample.getProductId(), agg.sample.getIdShop(), agg.sample.getSize());
            }
            refreshView();
        });
    }

    private void refreshView() {
        loadCartItems();
        if (onCartUpdated != null) onCartUpdated.run();
    }

    private static void stockLabelTooltip(Control c, String msg) {
        c.setTooltip(new Tooltip(msg));
    }

    private Label createSubtotalLabel(Aggregated agg) {
        Label sub = new Label(String.format("€ %.2f", agg.subtotal()));
        sub.setStyle("-fx-font-weight: bold;");
        return sub;
    }

    private Button createRemoveAllButton(Product p) {
        Button removeAll = new Button();
        removeAll.setPrefSize(24, 24);
        removeAll.setGraphic(new ImageView(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/basket.png")),
                        16, 16, true, true)));

        removeAll.setOnAction(ignored -> {
            appController.removeLine(p.getProductId(), p.getIdShop(), p.getSize());
            refreshView();
        });

        return removeAll;
    }

    @FXML
    private void onClearCart() {
        appController.clearCart();
        refreshView();
    }

}