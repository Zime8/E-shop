package org.example.boundary;

import javafx.application.Platform;
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
import org.example.control.BuyProductController;
import org.example.models.dto.CheckoutData;
import org.example.models.dto.CartRowData;
import org.example.util.ImageUtils;
import org.example.util.Navigator;

import java.util.*;

public class CartController {

    private static final String EURO_PRICE_FORMAT = "€ %.2f";

    private BuyProductController appController;
    private Navigator navigator;

    @FXML private VBox cartItemsContainer;
    @FXML private Label totalLabel;
    @FXML private Label emptyCartLabel;
    @FXML private ScrollPane cartScroll;
    private Runnable onCartUpdated;

    public void setOnCartUpdated(Runnable callback) { this.onCartUpdated = callback; }

    public void setAppController(BuyProductController app) {
        this.appController = app;
        if (cartItemsContainer != null) {
            refreshView();
        }
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public void refreshView() {
        if (appController == null || cartItemsContainer == null || totalLabel == null) {
            return;
        }

        List<CartRowData> rows = appController.loadCartRows();
        cartItemsContainer.getChildren().clear();

        boolean hasItems = !rows.isEmpty();
        toggleCartPlaceholders(hasItems);

        if (!hasItems) {
            emptyCartLabel.setVisible(true);
            totalLabel.setText("€ 0.00");
            return;
        }

        emptyCartLabel.setVisible(false);
        double total = rows.stream().mapToDouble(r -> r.agg().subtotal().doubleValue()).sum();
        totalLabel.setText(String.format(EURO_PRICE_FORMAT, total));
        rows.forEach(this::addRow);
    }

    private void addRow(CartRowData row) {
        GridPane gridRow = new GridPane();
        gridRow.setMinHeight(56);
        gridRow.setAlignment(Pos.CENTER);
        gridRow.setStyle("-fx-background-color: transparent; -fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 8; -fx-padding: 6;");

        configureRowGrid(gridRow);  // La tua funzione originale

        // Colonna 0: Immagine
        ImageView imgView = new ImageView();
        ImageUtils.setImage(imgView, row.productDto().imageData());
        imgView.setFitWidth(40);
        imgView.setFitHeight(40);
        imgView.setPreserveRatio(true);
        gridRow.add(imgView, 0, 0);

        // Colonna 1: Nome + taglia
        String sizeText = row.productDto().size() != null ? " (Taglia: " + row.productDto().size() + ")" : "";
        Label nameLabel = new Label(row.productDto().name() + sizeText);
        nameLabel.setStyle("-fx-font-weight: bold;");
        gridRow.add(nameLabel, 1, 0);

        // Colonna 2: Prezzo unitario
        Label unitLabel = new Label(String.format(EURO_PRICE_FORMAT, row.agg().unitPrice()));
        unitLabel.setStyle("-fx-text-fill: #d32f2f;");
        gridRow.add(unitLabel, 2, 0);

        // Colonna 3: Qty box
        HBox qtyBox = createSimpleQtyBox(row);
        gridRow.add(qtyBox, 3, 0);

        // Colonna 4: Subtotal
        Label subLabel = new Label(String.format(EURO_PRICE_FORMAT, row.agg().subtotal().doubleValue()));
        subLabel.setStyle("-fx-font-weight: bold;");
        gridRow.add(subLabel, 4, 0);

        // Colonna 5: Rimuovi
        Button removeBtn = new Button();
        removeBtn.setPrefSize(24, 24);
        removeBtn.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/basket.png")), 16, 16, true, true)));
        removeBtn.setOnAction(e -> {
            appController.removeLine(row.productDto().productId(), row.productDto().shopId(), row.productDto().size());
            refreshView();
            notifyCartUpdated();
        });
        gridRow.add(removeBtn, 5, 0);

        cartItemsContainer.getChildren().add(gridRow);
    }

    private HBox createSimpleQtyBox(CartRowData row) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER);

        Button minus = new Button("-");
        Label qtyLabel = new Label(String.valueOf(row.agg().qty()));
        Button plus = new Button("+");

        // Disable
        if (row.stockError()) {
            plus.setDisable(true);
            plus.setTooltip(new Tooltip("Quantità massima raggiunta: " + row.stock()));
        }

        minus.setOnAction(e -> {
            appController.changeQuantity(row.productDto().productId(), row.productDto().shopId(), row.productDto().size(), -1);
            Platform.runLater(() -> {
                refreshView();
                notifyCartUpdated();
            });
        });

        plus.setOnAction(e -> {
            appController.changeQuantity(row.productDto().productId(), row.productDto().shopId(), row.productDto().size(), +1);
            Platform.runLater(() -> {
                refreshView();
                notifyCartUpdated();
            });
        });

        box.getChildren().addAll(minus, qtyLabel, plus);
        return box;
    }

    @FXML private void onCheckout() {
        CheckoutData data = appController.buildCheckoutData();
        if (data.items().isEmpty()) return;

        navigator.openModal("/fxml/OrderSummary.fxml", (OrderSummaryController controller) -> {
            controller.setAppController(appController);
            controller.setNavigator(navigator);
            controller.loadData(data);
            controller.setOnCartUpdated(onCartUpdated);
        });
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

    private void configureRowGrid(GridPane row) {
        ColumnConstraints cImg = new ColumnConstraints();  cImg.setPercentWidth(12); cImg.setHalignment(HPos.CENTER);
        ColumnConstraints cName = new ColumnConstraints(); cName.setPercentWidth(33); cName.setHalignment(HPos.LEFT);
        ColumnConstraints cUnit = new ColumnConstraints(); cUnit.setPercentWidth(13); cUnit.setHalignment(HPos.CENTER);
        ColumnConstraints cQty = new ColumnConstraints();  cQty.setPercentWidth(15); cQty.setHalignment(HPos.CENTER);
        ColumnConstraints cSub = new ColumnConstraints();  cSub.setPercentWidth(17); cSub.setHalignment(HPos.CENTER);
        ColumnConstraints cRem = new ColumnConstraints();  cRem.setPercentWidth(10); cRem.setHalignment(HPos.CENTER);
        row.getColumnConstraints().addAll(cImg, cName, cUnit, cQty, cSub, cRem);
    }

    @FXML
    private void onClearCart() {
        appController.clearCart();
        refreshView();
        notifyCartUpdated();
    }

    private void notifyCartUpdated() {
        if (onCartUpdated != null) {
            onCartUpdated.run();
        }
    }

}