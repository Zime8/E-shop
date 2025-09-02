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
import org.example.models.CartItem;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderSummaryController {
    @FXML private VBox itemsBox;
    @FXML private Label totalLabel;

    private Stage stage;
    private List<CartItem> items;
    private BigDecimal total;

    // OrderSummaryController.java
    private Runnable onCartUpdated;

    private static final Logger logger = Logger.getLogger(OrderSummaryController.class.getName());

    public void setOnCartUpdated(Runnable onCartUpdated) {
        this.onCartUpdated = onCartUpdated;
    }
    public void setStage(Stage stage) { this.stage = stage; }

    public void setData(List<CartItem> items, BigDecimal total) {
        this.items = items;
        this.total = total;
        populate();
    }

    private void populate() {
        itemsBox.getChildren().clear();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);

        // Griglia unica per tutte le righe
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.prefWidthProperty().bind(itemsBox.widthProperty());
        grid.setMaxWidth(Double.MAX_VALUE);

        // Colonne: [img fissa] [nome elastica] [qty fissa] [subtotal fissa]
        ColumnConstraints cImg = new ColumnConstraints();
        cImg.setMinWidth(56); cImg.setPrefWidth(56); cImg.setMaxWidth(56);
        cImg.setHalignment(HPos.CENTER);

        ColumnConstraints cName = new ColumnConstraints();
        cName.setHgrow(Priority.ALWAYS); // prende tutto lo spazio che resta

        ColumnConstraints cQty = new ColumnConstraints();
        cQty.setMinWidth(56); cQty.setPrefWidth(56); cQty.setMaxWidth(56);
        cQty.setHalignment(HPos.CENTER);

        ColumnConstraints cSub = new ColumnConstraints();
        cSub.setMinWidth(110); cSub.setPrefWidth(110); cSub.setMaxWidth(120);
        cSub.setHalignment(HPos.RIGHT);

        grid.getColumnConstraints().addAll(cImg, cName, cQty, cSub);

        BigDecimal totalSum = BigDecimal.ZERO;
        int r = 0;

        for (CartItem it : items) {
            // immagine 56x56 (centrata)
            ImageView iv = getImageView(it);

            StackPane thumb = new StackPane(iv);
            thumb.setMinSize(56,56); thumb.setPrefSize(56,56); thumb.setMaxSize(56,56);
            thumb.setAlignment(Pos.CENTER);

            // nome (colonna elastica)
            Label name = new Label(it.getProductName() + " (" + it.getSize() + ")");
            name.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-text-fill:#222;");
            name.setWrapText(true);
            name.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(name, Priority.ALWAYS);

            // qty stretta
            int qty = it.getQuantity();
            Label qtyLbl = new Label("x " + qty);
            qtyLbl.setStyle("-fx-font-size:13; -fx-text-fill:#444;");
            qtyLbl.setAlignment(Pos.CENTER);

            // subtotal (calcolo)
            BigDecimal unit;
            try {
                Number u = it.getUnitPrice();
                if (u == null) {
                    unit = BigDecimal.ZERO;
                } else {
                    unit = BigDecimal.valueOf(u.doubleValue());
                }
            } catch (Exception ex) {
                unit = BigDecimal.ZERO;
            }
            BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(qty));
            totalSum = totalSum.add(subtotal);

            Label subtotalLbl = new Label(fmt.format(subtotal));
            subtotalLbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#d32f2f; -fx-font-size:14;");
            subtotalLbl.setAlignment(Pos.CENTER_RIGHT);

            // aggiungi nella griglia: stesse colonne per ogni riga
            grid.add(thumb,       0, r);
            grid.add(name,        1, r);
            grid.add(qtyLbl,      2, r);
            grid.add(subtotalLbl, 3, r);

            // allineamento verticale al centro per ogni cella
            GridPane.setValignment(thumb,       VPos.CENTER);
            GridPane.setValignment(name,        VPos.CENTER);
            GridPane.setValignment(qtyLbl,      VPos.CENTER);
            GridPane.setValignment(subtotalLbl, VPos.CENTER);

            r++;
        }

        itemsBox.getChildren().add(grid);

        total = totalSum;
        totalLabel.setText(fmt.format(totalSum)); // evita doppia "€"
    }

    private static ImageView getImageView(CartItem it) {
        ImageView iv = new ImageView();
        iv.setFitWidth(56);
        iv.setFitHeight(56);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        try {
            Object imgObj = it.getProductImage(); // Image o String URL
            if (imgObj instanceof Image image) {
                iv.setImage(image);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, String.format("Errore nel caricamento immagine prodotto '%s'", it.getProductName()), e);
        }

        return iv;
    }

    @FXML
    private void onBack() {
        if (stage != null) stage.close();
        else if (totalLabel != null && totalLabel.getScene() != null) ((Stage) totalLabel.getScene().getWindow()).close();
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

            ctrl.setData(items, total);

            payStage.showAndWait();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore durante l'apertura della schermata di pagamento", e);
            new Alert(Alert.AlertType.ERROR, "Errore nell'apertura della schermata di pagamento: " + e.getMessage());
        }
    }

}
