package org.example.controllers.ui;

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
import org.example.controllers.app.OrderSummaryAppController;
import org.example.controllers.app.OrderSummaryAppController.DisplayData;
import org.example.controllers.app.PaymentSelectionAppController;
import org.example.gateway.FakePaymentGateway;
import org.example.models.CartItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderSummaryController {
    @FXML private VBox itemsBox;
    @FXML private Label totalLabel;  // Assumi sia @FXML, aggiunto se mancante

    private Stage stage;
    private OrderSummaryAppController appController;
    private Runnable onCartUpdated;
    private List<CartItem> cartItems;
    private DisplayData currentViewData;

    private static final Logger logger = Logger.getLogger(OrderSummaryController.class.getName());

    public void setOnCartUpdated(Runnable onCartUpdated) {
        this.onCartUpdated = onCartUpdated;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML public void initialize(){
        appController = new OrderSummaryAppController();
    }

    public void loadData(List<CartItem> items, BigDecimal total) {
        this.cartItems = (items != null) ? List.copyOf(items) : List.of();
        if (appController != null) {
            currentViewData = appController.processItemsForDisplay(items, total);
            populateUI(currentViewData);
        }
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
        if (cartItems == null || cartItems.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Nessun dato carrello disponibile.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PaymentSelection.fxml"));
            Parent root = loader.load();
            var ctrl = (PaymentSelectionController)loader.getController();

            ctrl.setOnCartUpdated(this.onCartUpdated);

            PaymentSelectionAppController paymentAppCtrl = new PaymentSelectionAppController(
                    new FakePaymentGateway(1000L, 0.10));
            ctrl.setController(paymentAppCtrl);  // Passa appController
            ctrl.setData(cartItems, currentViewData.total());

            Stage payStage = new Stage();
            payStage.initOwner(stage != null ? stage.getOwner() : totalLabel.getScene().getWindow());
            payStage.initModality(Modality.APPLICATION_MODAL);
            ctrl.setStage(payStage);
            ctrl.setParentStage(this.stage);
            payStage.setScene(new Scene(root));

            payStage.showAndWait();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore durante l'apertura della schermata di pagamento", e);
            new Alert(Alert.AlertType.ERROR, "Errore nell'apertura della schermata di pagamento: " + e.getMessage()).showAndWait();
        }
    }

    private void populateUI(DisplayData viewData) {
        itemsBox.getChildren().clear();
        var fmt = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.ITALY);
        totalLabel.setText(fmt.format(viewData != null ? viewData.total() : BigDecimal.ZERO));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.prefWidthProperty().bind(itemsBox.widthProperty());
        grid.setMaxWidth(Double.MAX_VALUE);

        // Column constraints
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

        List<OrderSummaryAppController.ItemView> safeRows =
                (viewData == null || viewData.rows() == null) ? List.of() : viewData.rows();

        int r = 0;
        for (var it : safeRows) {
            // Immagine
            ImageView iv = getImageView(it.imageObj());
            StackPane thumb = new StackPane(iv);
            thumb.setMinSize(56,56); thumb.setPrefSize(56,56); thumb.setMaxSize(56,56);
            thumb.setAlignment(Pos.CENTER);

            // Nome
            Label name = new Label(it.productName() + (it.size() == null || it.size().isBlank() ? "" : " (" + it.size() + ")"));
            name.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-text-fill:#222;");
            name.setWrapText(true);
            name.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(name, Priority.ALWAYS);

            // Quantità
            Label qtyLbl = new Label("x " + it.quantity());
            qtyLbl.setStyle("-fx-font-size:13; -fx-text-fill:#444;");
            qtyLbl.setAlignment(Pos.CENTER);

            // Subtotal
            BigDecimal subtotal = it.unitPrice().multiply(BigDecimal.valueOf(it.quantity()));
            Label subtotalLbl = new Label(fmt.format(subtotal));
            subtotalLbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#d32f2f; -fx-font-size:14;");
            subtotalLbl.setAlignment(Pos.CENTER_RIGHT);

            // Aggiungi alla griglia
            grid.add(thumb, 0, r);
            grid.add(name, 1, r);
            grid.add(qtyLbl, 2, r);
            grid.add(subtotalLbl, 3, r);

            GridPane.setValignment(thumb, VPos.CENTER);
            GridPane.setValignment(name, VPos.CENTER);
            GridPane.setValignment(qtyLbl, VPos.CENTER);
            GridPane.setValignment(subtotalLbl, VPos.CENTER);

            r++;
        }
        itemsBox.getChildren().add(grid);
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

    private static Image toImage(Object src) {
        return switch (src) {
            case null -> null;
            case Image image -> image;
            case byte[] bytes -> bytes.length == 0 ? null : new Image(new ByteArrayInputStream(bytes));
            case CharSequence cs -> {
                String s = cs.toString().trim();
                yield s.isEmpty() ? null : new Image(s, true);
            }
            default -> {
                Logger.getLogger(OrderSummaryController.class.getName())
                        .fine(() -> "Tipo di immagine non supportata: " + src.getClass());
                yield null;
            }
        };
    }

}
