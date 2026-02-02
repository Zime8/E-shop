package org.example.controllers.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import org.example.controllers.app.CartAppController;
import org.example.controllers.app.CartAppController.Key;
import org.example.controllers.app.CartAppController.Aggregated;
import org.example.controllers.app.CartAppController.CheckoutData;
import org.example.models.Product;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CartController {

    private CartAppController appController;

    @FXML private VBox cartItemsContainer;
    @FXML private Label totalLabel;
    @FXML private Label emptyCartLabel;
    @FXML private ScrollPane cartScroll;
    private Runnable onCartUpdated;

    private static final Logger logger = Logger.getLogger(CartController.class.getName());
    private static final String MSG_STOCK_UNKNOWN = "Disponibilità non verificabile";

    public void setOnCartUpdated(Runnable callback) { this.onCartUpdated = callback; }

    public void initialize() {
        appController = new CartAppController();
        loadCartItems();
    }

    // Carica i prodotti nel carrello
    public void loadCartItems() {
        cartItemsContainer.getChildren().clear();
        try {
            CheckoutData data = appController.buildCheckoutData();  // Tutto in 1 chiamata!
            List<Product> cartItems = appController.getCartItems();
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

    private HBox createQtyBox(Product p, Aggregated agg) {
        HBox qtyBox = new HBox(8);
        qtyBox.setAlignment(Pos.CENTER);

        Button minus = new Button("-");
        Label qtyLbl = new Label(String.valueOf(agg.getQty()));
        Button plus = new Button("+");

        try {
            int stock = appController.getStockFor(p.getProductId(), p.getIdShop(), p.getSize());
            setupNormalQtyBox(minus, plus, stock, agg.getQty(), p);
        } catch (Exception ex) {
            // In caso di errore niente incremento
            logger.log(Level.WARNING, ex, () -> "Impossibile leggere lo stock per " + p.getName());
            setupErrorQtyBox(minus, plus, qtyLbl, p);
        }

        qtyBox.getChildren().addAll(minus, qtyLbl, plus);
        return qtyBox;
    }

    private void setupNormalQtyBox(Button minus, Button plus, int stock, int currentQty, Product p) {
        if (currentQty >= stock) {
            plus.setDisable(true);
            stockLabelTooltip(plus, "Quantità massima raggiunta: " + stock);
        }

        minus.setOnAction(e -> {
                appController.changeQuantity(p, -1);
                refreshView();
        });
        plus.setOnAction(e -> {
            appController.changeQuantity(p, +1);
            refreshView();
        });
    }

    private void setupErrorQtyBox(Button minus, Button plus, Label qtyLbl, Product p) {
        plus.setDisable(true);
        stockLabelTooltip(plus, MSG_STOCK_UNKNOWN);
        stockLabelTooltip(qtyLbl, MSG_STOCK_UNKNOWN);
        stockLabelTooltip(minus, MSG_STOCK_UNKNOWN);
        minus.setOnAction(e -> {
            appController.changeQuantity(p, -1);
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

    @FXML private void onCheckout() {
        try {
            CheckoutData data = appController.buildCheckoutData();  // ← DELEGA
            if (data.items().isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Il carrello è vuoto.").showAndWait();
                return;
            }
            openOrderSummary(data);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore checkout", e);
            new Alert(Alert.AlertType.ERROR, "Errore checkout: " + e.getMessage()).showAndWait();
        }
    }

    // Trova la finestra owner corretta per la dialog
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OrderSummary.fxml"));
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
            ctrl.loadData(data.items(), data.total());

            dialog.showAndWait();
            refreshView();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossibile aprire il riepilogo ordine", e);
            new Alert(Alert.AlertType.ERROR,
                    "Impossibile aprire il riepilogo ordine: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onClearCart() {
        appController.clearCart();
        refreshView();
    }

}
