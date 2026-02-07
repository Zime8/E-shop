package org.example.controllers.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.controllers.app.ProductDetailAppController;
import org.example.controllers.app.ReviewAppController;
import org.example.models.Product;
import org.example.models.Shop;
import org.example.util.Session;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    private ProductDetailAppController appController;
    private Product product;
    private Runnable onCartUpdate;

    public void setOnCartUpdate(Runnable callback) {
        this.onCartUpdate = callback;
    }

    public void setAppController(ProductDetailAppController app) {
        this.appController = app;
    }

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
                product.setSize(sel);
                refreshForSelectedSize(sel);

                sizeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newSel) -> {
                    if (newSel == null) return;
                    product.setSize(newSel);
                    try {
                        refreshForSelectedSize(newSel);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Errore aggiornando per taglia " + newSel, e);
                    }
                });
            } else {
                sizeCombo.setDisable(true);
                updateStockAndQtyRange();
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

    @FXML private void onAddToCart() {
        if (product == null) return;

        applySelectedSizeIfPresent();

        int qty = getSelectedQtyOrDefault();
        int max = getMaxAvailableQtySafe();
        if (!appController.isValidQuantity(qty, max)) {
            showQtyExceededWarning(max);
            return;
        }

        appController.addToCart(product, qty);
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
            if (!ensureSizeSelectedOrWarn()) return;

            appController.addToWishList(Session.getUser(), product.getProductId(), product.getIdShop(), product.getSize());
            addToWishListBtn.setDisable(true);
            addToWishListBtn.setText(TXT_ADDED_TO_WISHLIST);

        } catch (Exception e) {
            showError("Impossibile aggiungere ai preferiti:\n" + e.getMessage(), e);
        }
    }

    @FXML private void onAddReview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListReview.fxml"));
            Parent root = loader.load();
            ReviewController ctrl = loader.getController();
            ctrl.setAppController(new ReviewAppController());
            ctrl.init(product);

            Stage stage = new Stage();
            stage.setTitle("Recensioni prodotto");
            stage.initOwner(addReview.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException ex) {
            showError("Impossibile aprire recensioni:\n" + ex.getMessage(), ex);
        }
    }

    @FXML private void onClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    // Metodi UI privati invariati
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

        Stage stage = new Stage();
        stage.setTitle("Informazioni negozio");
        stage.initOwner(nameShop.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(root));
        stage.showAndWait();
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

    private void updateStockAndQtyRange() {
        try {
            Integer stock;
            String sel = sizeCombo.getValue();

            stock = appController.getStockFor(product.getProductId(), product.getIdShop(), sel);

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
        product.setPrice(priceSel);
        priceLbl.setText(String.format(EUR_PRICE_FMT, priceSel));

        boolean wished = appController.existsWish(Session.getUser(), product.getProductId(), product.getIdShop(), sel);
        updateWishButton(wished);

        updateStockAndQtyRange();
    }

    // Helper UI
    private void applySelectedSizeIfPresent() {
        if (sizeCombo != null && sizeCombo.getValue() != null) {
            product.setSize(sizeCombo.getValue());
        }
    }

    private int getSelectedQtyOrDefault() {
        return qtySpinner != null && qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
    }

    private int getMaxAvailableQtySafe() {
        try {
            var vf = qtySpinner.getValueFactory();
            if (vf instanceof SpinnerValueFactory.IntegerSpinnerValueFactory ivf) {
                return ivf.getMax();
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Max qty error", e);
        }
        return Integer.MAX_VALUE;
    }

    private void showQtyExceededWarning(int max) {
        new Alert(Alert.AlertType.WARNING, "Quantità non disponibile. Max: " + max).showAndWait();
    }

    private void closeWindow(Control control) {
        try {
            Stage stage = (Stage) control.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Close window error", e);
        }
    }

    private boolean ensureSizeSelectedOrWarn() {
        String selSize = sizeCombo != null ? sizeCombo.getValue() : null;
        if (selSize == null || selSize.isBlank()) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Taglia mancante");
            a.setContentText("Seleziona una taglia prima di procedere.");
            a.showAndWait();
            return false;
        }
        product.setSize(selSize);
        return true;
    }

    private void showError(String message, Throwable t) {
        logger.log(Level.WARNING, message, t);
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
