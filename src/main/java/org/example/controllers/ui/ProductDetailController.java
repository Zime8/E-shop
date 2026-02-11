package org.example.controllers.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.controllers.app.ProductDetailAppController;
import org.example.models.CartItem;
import org.example.models.Product;
import org.example.models.Shop;
import org.example.util.Navigator;
import org.example.util.Session;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDetailController {

    private static final String EUR_PRICE_FMT = "€ %.2f";
    private static final String TXT_ADDED_TO_WISHLIST = "Aggiunto ai preferiti";
    private static final String STYLE = "-fx-font-weight: bold;";
    private static final Logger logger = Logger.getLogger(ProductDetailController.class.getName());

    @FXML private ImageView bigPhoto;
    @FXML private Label nameLbl;
    @FXML private Label nameShop;
    @FXML private Label priceLbl;
    @FXML private Button closeBtn;
    @FXML private Button addToCartBtn;
    @FXML private Button addToWishListBtn;
    @FXML private Button addReview;
    @FXML private ComboBox<String> sizeCombo;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private Label stockLabel;
    @FXML private Pane rootPane;

    private ProductDetailAppController appController;
    private Product product;
    private Runnable onCartUpdate;
    private Stage stage;

    @SuppressWarnings("unused") // Navigator
    public void setOnCartUpdate(Runnable callback) {
        this.onCartUpdate = callback;
    }

    public void setAppController(Object app) {
        this.appController = (ProductDetailAppController) app;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        setupPopupBackdrop();
    }

    private void setupPopupBackdrop() {
        if (stage == null || rootPane == null) return;

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        Scene scene = stage.getScene();
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        rootPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getTarget() == rootPane) {
                stage.close();
            }
        });

        // Posiziona centro schermo
        stage.setX((javafx.stage.Screen.getPrimary().getBounds().getWidth() - 450) / 2);
        stage.setY((javafx.stage.Screen.getPrimary().getBounds().getHeight() - 500) / 2);
    }

    @SuppressWarnings("unused")
    public void loadData(Object dataObj) {
        if (dataObj instanceof Product p) {
            setProduct(p);
        }
    }

    // Carica le informazioni della schermata relative al prodotto selezionato
    public void setProduct(Product p) {
        this.product = p;

        updateImage(p.getImageData());
        nameLbl.setText(p.getName());
        setupShopLabel();
        priceLbl.setText(String.format(EUR_PRICE_FMT, p.getPrice()));
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        qtySpinner.setEditable(true);

        try {
            List<String> sizes = appController.getAvailableSizes(p.getProductId(), p.getIdShop());
            sizeCombo.getItems().setAll(sizes);

            if (!sizes.isEmpty()) {
                sizeCombo.getSelectionModel().selectFirst();
                String sel = sizeCombo.getValue();
                refreshForSelectedSize(sel);

                sizeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newSel) -> {
                    if (newSel == null) return;
                    try {
                        refreshForSelectedSize(newSel);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Errore aggiornando per taglia " + newSel, e);
                    }
                });
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Errore caricando taglie", ex);
        }

        try {
            boolean alreadyWished = appController.existsWish(Session.getUser(), product.getProductId(), product.getIdShop());
            updateWishButton(alreadyWished);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Errore controllo wishlist", ex);
        }
    }

    // Aggiunta del prodotto al carrello
    @FXML private void onAddToCart() {
        if (product == null) return;

        String size = sizeCombo.getValue();
        if (size == null || size.isBlank()) {
            showSizeWarning();
            return;
        }

        int qty = getSelectedQtyOrDefault();
        int max = getMaxAvailableQtySafe(size);
        if (!appController.isValidQuantity(qty, max)) {
            showQtyExceededWarning(max);
            return;
        }

        CartItem item = new CartItem(
                product.getProductId(), product.getIdShop(), qty,
                product.getPrice(), product.getName(),
                product.getImageData(), size
        );

        appController.addToCart(item);
        if (onCartUpdate != null) {
            Platform.runLater(() -> {
                onCartUpdate.run();
                closeWindow(addToCartBtn);
            });
        } else {
            closeWindow(addToCartBtn);
        }
    }

    @FXML private void addToWishList() {
        try {
            String size = sizeCombo.getValue();
            if (size == null || size.isBlank()) {
                showSizeWarning();
                return;
            }

            appController.addToWishList(Session.getUser(), product.getProductId(), product.getIdShop(), size);
            addToWishListBtn.setDisable(true);
            addToWishListBtn.setText(TXT_ADDED_TO_WISHLIST);

        } catch (Exception e) {
            showError("Impossibile aggiungere ai preferiti:\n" + e.getMessage(), e);
        }
    }

    @FXML private void onAddReview() {
        try {
            Navigator.openModal("/fxml/ListReview.fxml", product, null);
        } catch (Exception ex) {
            showError("Impossibile aprire recensioni:\n" + ex.getMessage(), ex);
        }
    }

    @FXML private void onClose() {
        if (this.stage != null) this.stage.close();
    }

    private void updateImage(byte[] data) {
        if (data != null && data.length > 0) {
            bigPhoto.setImage(new Image(new ByteArrayInputStream(data)));
        } else {
            bigPhoto.setImage(null);
        }
    }

    private void setupShopLabel() {
        nameShop.setText(product.getNameShop());
        nameShop.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1a73e8; -fx-underline: true; -fx-cursor: hand;");
        nameShop.setTooltip(new Tooltip("Vedi informazioni negozio"));
        nameShop.setOnMouseClicked(e -> onShopClick());
    }

    private void onShopClick() {
        if (product == null) return;

        try {
            Shop shop = appController.getShopInfo(product.getIdShop());
            if (shop == null) {
                new Alert(Alert.AlertType.INFORMATION, "Informazioni negozio non disponibili.").showAndWait();
                return;
            }
            showShopDialog(shop);
        } catch (Exception ex) {
            showError("Impossibile caricare info negozio:\n" + ex.getMessage(), ex);
        }
    }

    private void showShopDialog(Shop shop) {
        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #fff; -fx-border-color: #d32f2f; -fx-border-width: 2; -fx-background-radius: 14; -fx-border-radius: 14;");
        root.setPadding(new Insets(18));

        Label title = new Label("Informazioni negozio");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setMinWidth(90);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        Label lName = new Label("Nome:"); lName.setStyle(STYLE);
        Label vName = new Label(shop.getName() != null ? shop.getName() : nameShop.getText());

        Label lAddress = new Label("Via:"); lAddress.setStyle(STYLE);
        Hyperlink vAddress = new Hyperlink(shop.getAddress() != null ? shop.getAddress() : "-");
        vAddress.setOnAction(e -> openMapsForAddress(vAddress.getText()));

        Label lTel = new Label("Telefono:"); lTel.setStyle(STYLE);
        Label vTel = new Label(shop.getPhone() != null ? shop.getPhone() : "-");

        grid.addRow(0, lName, vName);
        grid.addRow(1, lAddress, vAddress);
        grid.addRow(2, lTel, vTel);

        Button close = new Button("Chiudi");
        close.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 6 14;");
        close.setOnAction(e -> ((Stage) close.getScene().getWindow()).close());

        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, grid, footer);

        Stage dialogStage = new Stage();
        dialogStage.setTitle("Informazioni negozio");
        dialogStage.initOwner(nameShop.getScene().getWindow());
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setScene(new Scene(root));
        dialogStage.showAndWait();
    }

    private void openMapsForAddress(String address) {
        if (address == null || address.isBlank()) {
            new Alert(Alert.AlertType.INFORMATION, "Indirizzo non disponibile.").showAndWait();
            return;
        }
        try {
            String q = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://www.google.com/maps/search/?api=1&query=" + q;

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Apri manualmente:\n" + url).showAndWait();
            }
        } catch (Exception ex) {
            showError("Impossibile aprire Google Maps:\n" + ex.getMessage(), ex);
        }
    }

    private void updateWishButton(boolean already) {
        if (already) {
            addToWishListBtn.setText(TXT_ADDED_TO_WISHLIST);
            addToWishListBtn.setDisable(true);
        } else {
            addToWishListBtn.setText("🌟  Preferiti");
            addToWishListBtn.setDisable(false);
        }
    }

    private void updateStockAndQtyRange(String selSize) {
        try {
            Integer stock = appController.getStockFor(product.getProductId(), product.getIdShop(), selSize);

            int max = stock != null ? stock : 0;

            if (max <= 0) {
                stockLabel.setText("Esaurito");
                qtySpinner.setDisable(true);
                addToCartBtn.setDisable(true);
            } else {
                stockLabel.setText(String.valueOf(max));
                qtySpinner.setDisable(false);
                addToCartBtn.setDisable(false);

                int current = qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
                qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, Math.min(current, max)));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore stock", e);
            stockLabel.setText("Disponibilità: —");
            qtySpinner.setDisable(false);
            addToCartBtn.setDisable(false);
            qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        }
    }

    private void refreshForSelectedSize(String sel) {
        double priceSel = appController.getPriceFor(product.getProductId(), product.getIdShop(), sel);
        priceLbl.setText(String.format(EUR_PRICE_FMT, priceSel));

        boolean wished = appController.existsWish(Session.getUser(), product.getProductId(), product.getIdShop(), sel);
        updateWishButton(wished);

        updateStockAndQtyRange(sel);
    }

    private int getSelectedQtyOrDefault() {
        return qtySpinner != null && qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
    }

    private int getMaxAvailableQtySafe(String size) {
        try {
            return appController.getStockFor(product.getProductId(), product.getIdShop(), size);
        } catch (Exception e) {
            logger.warning("Errore stock: " + e.getMessage());
            return 10;
        }
    }

    private void showQtyExceededWarning(int max) {
        new Alert(Alert.AlertType.WARNING, "Quantità non disponibile. Max: " + max).showAndWait();
    }

    private void closeWindow(Control control) {
        try {
            Stage windowStage = (Stage) control.getScene().getWindow();
            windowStage.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Close window error", e);
        }
    }

    private void showSizeWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText("Seleziona una taglia.");
        alert.showAndWait();
    }

    private void showError(String message, Throwable t) {
        logger.log(Level.WARNING, message, t);
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
