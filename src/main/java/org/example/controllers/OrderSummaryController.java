package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.dao.OrderDAO;
import org.example.models.CartItem;
import org.example.models.Order;
import org.example.models.OrderLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderSummaryController {
    @FXML private VBox itemsBox;
    @FXML private Label totalLabel;

    private Stage stage;
    private List<CartItem> items; // mantenuto per compatibilità con il flusso "carrello -> pagamento"
    private BigDecimal total;

    private Runnable onCartUpdated;

    private static final Logger logger = Logger.getLogger(OrderSummaryController.class.getName());

        // ======= Nuovo: view-model neutro per visualizzare righe sia da CartItem che da OrderLine =======
        private record ItemView(String productName, String size, int quantity, BigDecimal unitPrice, Object imageObj) {
            private ItemView(String productName, String size, int quantity, BigDecimal unitPrice, Object imageObj) {
                this.productName = productName;
                this.size = size;
                this.quantity = quantity;
                this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
                this.imageObj = imageObj;
            }
        }

    public void setOnCartUpdated(Runnable onCartUpdated) {
        this.onCartUpdated = onCartUpdated;
    }
    public void setStage(Stage stage) { this.stage = stage; }

    // =======================
    // 1) Flusso esistente: dal carrello (CartItem)
    // =======================
    public void setData(List<CartItem> items, BigDecimal total) {
        this.items = items;
        this.total = total;
        // Converte i CartItem nelle righe neutre e popola
        List<ItemView> rows = new ArrayList<>();
        if (items != null) {
            for (CartItem it : items) {
                BigDecimal unit = BigDecimal.ZERO;
                try {
                    Number u = it.getUnitPrice();
                    unit = (u == null) ? BigDecimal.ZERO : BigDecimal.valueOf(u.doubleValue());
                } catch (Exception ignore) {}
                rows.add(new ItemView(
                        it.getProductName(),
                        it.getSize(),
                        it.getQuantity(),
                        unit,
                        it.getProductImage() // può essere Image/byte[]/String URL
                ));
            }
        }
        populate(rows);
    }

    // =======================
    // 2) Nuovo: dal MODEL Order (prodotto sia in demo che in prod dai nuovi metodi DAO)
    // =======================
    public void setData(Order order) {
        if (order == null) {
            itemsBox.getChildren().setAll(new Label("Ordine non trovato."));
            totalLabel.setText(NumberFormat.getCurrencyInstance(Locale.ITALY).format(0));
            return;
        }
        List<ItemView> rows = new ArrayList<>();
        for (OrderLine l : order.getLines()) {
            rows.add(new ItemView(
                    safe(l.getProductName()),
                    safe(l.getSize()),
                    l.getQuantity(),
                    l.getUnitPrice(),
                    null // il model OrderLine non porta immagine; lasciamo null
            ));
        }
        this.total = order.getTotal();
        populate(rows);
    }

    // 3) Nuovo: carica direttamente da un orderId usando OrderDAO.getOrderModel(...)
    public void setData(int orderId) {
        try {
            Optional<Order> opt = OrderDAO.getOrderModel(orderId);
            if (opt.isEmpty()) {
                itemsBox.getChildren().setAll(new Label("Ordine #" + orderId + " non trovato."));
                totalLabel.setText(NumberFormat.getCurrencyInstance(Locale.ITALY).format(0));
                return;
            }
            setData(opt.get());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nel caricamento dell'ordine #" + orderId, e);
            itemsBox.getChildren().setAll(new Label("Impossibile caricare l'ordine."));
            totalLabel.setText(NumberFormat.getCurrencyInstance(Locale.ITALY).format(0));
        }
    }

    // 4) Nuovo: subito dopo placeOrderWithStockDecrement(...) — passami CreationResult e userId
    public void setData(OrderDAO.CreationResult creation, int userId) {
        if (creation == null || creation.orderIds() == null || creation.orderIds().isEmpty()) {
            itemsBox.getChildren().setAll(new Label("Nessun ordine generato."));
            totalLabel.setText(NumberFormat.getCurrencyInstance(Locale.ITALY).format(0));
            return;
        }
        try {
            // Recupera tutti gli ordini creati (potrebbero essere più di uno, uno per shop)
            List<ItemView> rows = new ArrayList<>();
            BigDecimal grandTotal = BigDecimal.ZERO;

            for (Integer orderId : creation.orderIds()) {
                Optional<Order> opt = OrderDAO.getOrderModel(orderId);
                if (opt.isEmpty()) continue;
                Order o = opt.get();
                grandTotal = grandTotal.add(o.getTotal());
                for (OrderLine l : o.getLines()) {
                    rows.add(new ItemView(
                            safe(l.getProductName()),
                            safe(l.getSize()),
                            l.getQuantity(),
                            l.getUnitPrice(),
                            null
                    ));
                }
            }
            this.total = grandTotal;
            populate(rows);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nel caricamento degli ordini generati", e);
            itemsBox.getChildren().setAll(new Label("Impossibile caricare il riepilogo degli ordini."));
            totalLabel.setText(NumberFormat.getCurrencyInstance(Locale.ITALY).format(0));
        }
    }

    // ======= rendering comune =======
    private void populate(List<ItemView> rows) {
        itemsBox.getChildren().clear();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.prefWidthProperty().bind(itemsBox.widthProperty());
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints cImg = new ColumnConstraints();
        cImg.setMinWidth(56); cImg.setPrefWidth(56); cImg.setMaxWidth(56);
        cImg.setHalignment(HPos.CENTER);

        ColumnConstraints cName = new ColumnConstraints();
        cName.setHgrow(Priority.ALWAYS);

        ColumnConstraints cQty = new ColumnConstraints();
        cQty.setMinWidth(56); cQty.setPrefWidth(56); cQty.setMaxWidth(56);
        cQty.setHalignment(HPos.CENTER);

        ColumnConstraints cSub = new ColumnConstraints();
        cSub.setMinWidth(110); cSub.setPrefWidth(110); cSub.setMaxWidth(120);
        cSub.setHalignment(HPos.RIGHT);

        grid.getColumnConstraints().addAll(cImg, cName, cQty, cSub);

        BigDecimal totalSum = BigDecimal.ZERO;
        int r = 0;

        List<ItemView> safeRows = (rows == null) ? Collections.emptyList() : rows;
        for (ItemView it : safeRows) {
            ImageView iv = getImageView(it.imageObj);

            StackPane thumb = new StackPane(iv);
            thumb.setMinSize(56,56); thumb.setPrefSize(56,56); thumb.setMaxSize(56,56);
            thumb.setAlignment(Pos.CENTER);

            Label name = new Label(it.productName + (it.size == null || it.size.isBlank() ? "" : " (" + it.size + ")"));
            name.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-text-fill:#222;");
            name.setWrapText(true);
            name.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(name, Priority.ALWAYS);

            Label qtyLbl = new Label("x " + it.quantity);
            qtyLbl.setStyle("-fx-font-size:13; -fx-text-fill:#444;");
            qtyLbl.setAlignment(Pos.CENTER);

            BigDecimal subtotal = it.unitPrice.multiply(BigDecimal.valueOf(it.quantity));
            totalSum = totalSum.add(subtotal);

            Label subtotalLbl = new Label(fmt.format(subtotal));
            subtotalLbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#d32f2f; -fx-font-size:14;");
            subtotalLbl.setAlignment(Pos.CENTER_RIGHT);

            grid.add(thumb,       0, r);
            grid.add(name,        1, r);
            grid.add(qtyLbl,      2, r);
            grid.add(subtotalLbl, 3, r);

            GridPane.setValignment(thumb,       VPos.CENTER);
            GridPane.setValignment(name,        VPos.CENTER);
            GridPane.setValignment(qtyLbl,      VPos.CENTER);
            GridPane.setValignment(subtotalLbl, VPos.CENTER);

            r++;
        }

        itemsBox.getChildren().add(grid);

        // se total è già stato impostato (es. da Order.getTotal) uso quello; altrimenti uso totalSum
        if (this.total == null) this.total = totalSum;
        totalLabel.setText(fmt.format(this.total));
    }

    private ImageView getImageView(Object imgObj) {
        ImageView iv = new ImageView();
        iv.setFitWidth(56);
        iv.setFitHeight(56);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        try {
            iv.setImage(toImage(imgObj));
        } catch (Exception e) {
            logger.log(Level.FINE, "Errore nel caricamento immagine prodotto", e);
        }
        return iv;
    }

    // Versione compatibile con Java 8/11/17 (no pattern matching switch)
    private static Image toImage(Object src) {
        switch (src) {
            case null -> {
                return null;
            }
            case Image image -> {
                return image;
            }
            case byte[] bytes -> {
                if (bytes.length == 0) return null;
                return new Image(new ByteArrayInputStream(bytes));
            }
            case CharSequence cs -> {
                String s = cs.toString().trim();
                return s.isEmpty() ? null : new Image(s, true);
            }
            default -> {
                Logger.getLogger(OrderSummaryController.class.getName())
                        .fine(() -> "Tipo di immagine non supportata: " + src.getClass());
                return null;
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @FXML
    private void onBack() {
        if (stage != null) stage.close();
        else if (totalLabel != null && totalLabel.getScene() != null) {
            ((Stage) totalLabel.getScene().getWindow()).close();
        }
    }

    @FXML
    private void onPay() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PaymentSelection.fxml"));
            Parent root = loader.load();
            PaymentSelectionController ctrl = loader.getController();

            ctrl.setOnCartUpdated(this.onCartUpdated);

            Stage payStage = new Stage();
            payStage.initOwner(stage != null ? stage.getOwner() : totalLabel.getScene().getWindow());
            payStage.initModality(Modality.APPLICATION_MODAL);
            ctrl.setStage(payStage);
            ctrl.setParentStage(this.stage);
            payStage.setScene(new Scene(root));

            // ✅ Manteniamo il flusso storico: passo items/total se sto ancora pagando
            ctrl.setData(items, total);

            payStage.showAndWait();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore durante l'apertura della schermata di pagamento", e);
            new Alert(Alert.AlertType.ERROR, "Errore nell'apertura della schermata di pagamento: " + e.getMessage()).showAndWait();
        }
    }
}
