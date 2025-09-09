package org.example.controllers;

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
import org.example.dao.ProductDaos;
import org.example.dao.ShopDAO;
import org.example.dao.UserDAO;
import org.example.dao.api.ProductDao;
import org.example.models.Product;
import org.example.models.Shop;
import org.example.util.Session;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDetailController {

    private final ProductDao productDao;
    public ProductDetailController(ProductDao productDao) {
        this.productDao = productDao;
    }

    public ProductDetailController(){
        this(ProductDaos.create());
    }

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
    private Product product;
    private Runnable onAddToCartCallback;

    private static final String EUR_PRICE_FMT = "€ %.2f";
    private static final String TXT_ADDED_TO_WISHLIST = "Aggiunto ai preferiti";

    private static final Logger logger = Logger.getLogger(ProductDetailController.class.getName());
    private static final String STYLE = "-fx-font-weight: bold;";

    public void setOnAddToCartCallback(Runnable callback) {
        this.onAddToCartCallback = callback;
    }

    public void setProduct(Product p) throws SQLException {
        this.product = p;
        byte[] data = p.getImageData();
        if (data != null && data.length > 0) {
            bigPhoto.setImage(new Image(new ByteArrayInputStream(data)));
        } else {
            bigPhoto.setImage(null);
        }
        nameLbl.setText(p.getName());

        nameShop.setText(p.getNameShop());
        nameShop.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1a73e8; -fx-underline: true; -fx-cursor: hand;");
        nameShop.setTooltip(new Tooltip("Vedi informazioni negozio"));
        nameShop.setOnMouseClicked(e -> onShopClick());

        priceLbl.setText(String.format(EUR_PRICE_FMT, p.getPrice()));
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        qtySpinner.setEditable(true);

        try {
            List<String> sizes = productDao.getAvailableSizes(p.getProductId(), p.getIdShop());
            sizeCombo.getItems().setAll(sizes);

            if (!sizes.isEmpty()) {
                sizeCombo.getSelectionModel().selectFirst();
                String sel = sizeCombo.getValue();
                product.setSize(sel);
                refreshForSelectedSize(sel);

                // Listener: ogni cambio taglia → un solo punto di verità
                sizeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newSel) -> {
                    if (newSel == null) return;
                    product.setSize(newSel);
                    try {
                        refreshForSelectedSize(newSel);
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, e,
                                () -> "Errore aggiornando prezzo/stato preferiti per taglia " + newSel);
                    }
                });

            } else {
                sizeCombo.setDisable(true);
                updateStockAndQtyRange();
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Errore caricando taglie/prezzo", ex);
        }

        // stato wishlist “senza taglia” (se lo mantieni come logica)
        boolean alreadyWished = productDao.existsWish(Session.getUser(),
                product.getProductId(),
                product.getIdShop());
        updateWishButton(alreadyWished);
    }

    private void onShopClick() {
        if (product == null) return;

        try {
            Shop shop = ShopDAO.getById(product.getIdShop());
            if (shop == null) {
                new Alert(Alert.AlertType.INFORMATION, "Informazioni negozio non disponibili.").showAndWait();
                return;
            }

            VBox root = new VBox(12);
            root.setStyle("""
            -fx-background-color: #fff;
            -fx-border-color: #d32f2f;
            -fx-border-width: 2;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
        """);
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
            vAddress.setOnAction(e2 -> openMapsForAddress(vAddress.getText()));

            Label lTel = new Label("Telefono:"); lTel.setStyle(STYLE);
            Label vTel = new Label(shop.getPhone() != null ? shop.getPhone() : "-");

            grid.addRow(0, lName, vName);
            grid.addRow(1, lAddress, vAddress);
            grid.addRow(2, lTel, vTel);

            Button close = new Button("Chiudi");
            close.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 6 14;");
            close.setAlignment(Pos.CENTER);
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

        } catch (Exception ex) {
            showError("Impossibile aprire le informazioni del negozio:\n" + ex.getMessage(), ex);
        }
    }

    // Apre l'indirizzo su google maps
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
            String sel = (sizeCombo != null) ? sizeCombo.getValue() : null;

            stock = productDao.getStockFor(product.getProductId(), product.getIdShop(), sel);

            int max = stock;

            if (max <= 0) {
                stockLabel.setText("Esaurito");
                qtySpinner.setDisable(true);
                addToCartBtn.setDisable(true);
            } else {
                stockLabel.setText(String.valueOf(max));
                qtySpinner.setDisable(false);
                addToCartBtn.setDisable(false);

                int current = (qtySpinner.getValue() != null) ? qtySpinner.getValue() : 1;
                qtySpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, Math.min(current, max))
                );
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore recuperando disponibilità", e);
            stockLabel.setText("Disponibilità: —");
            qtySpinner.setDisable(false);
            addToCartBtn.setDisable(false);
            qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        }
    }


    @FXML
    private void onAddToCart() {
        if (product == null) return;

        applySelectedSizeIfPresent();
        logger.log(Level.INFO, "Sto aggiungendo al carrello: {0}", product.getName());

        int qty = getSelectedQtyOrDefault();
        int max = getMaxAvailableQtySafe();
        if (qty > max) {
            showQtyExceededWarning(max);
            return;
        }

        addToCartTimes(qty);
        notifyCartUpdated();
        logger.log(Level.INFO, "Prodotto aggiunto al carrello: {0}", product.getName());
        closeWindow(addToCartBtn);
    }

    private void applySelectedSizeIfPresent() {
        if (sizeCombo != null && sizeCombo.getValue() != null) {
            product.setSize(sizeCombo.getValue());
        }
    }

    private int getSelectedQtyOrDefault() {
        return (qtySpinner != null && qtySpinner.getValue() != null) ? qtySpinner.getValue() : 1;
    }

    private int getMaxAvailableQtySafe() {
        try {
            var vf = Objects.requireNonNull(qtySpinner).getValueFactory();
            if (vf instanceof SpinnerValueFactory.IntegerSpinnerValueFactory ivf) {
                return ivf.getMax();
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Impossibile determinare il massimo consentito", e);
        }
        return Integer.MAX_VALUE;
    }

    private void showQtyExceededWarning(int max) {
        new Alert(Alert.AlertType.WARNING, "Quantità selezionata non disponibile. Max: " + max).showAndWait();
    }

    private void addToCartTimes(int qty) {
        for (int i = 0; i < qty; i++) {
            Session.addToCart(Product.copyOf(product));
        }
    }

    private void notifyCartUpdated() {
        if (onAddToCartCallback != null) onAddToCartCallback.run();
    }

    private void closeWindow(Control control) {
        try {
            Stage stage = (Stage) control.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore chiudendo la finestra dopo l'aggiunta al carrello", e);
        }
    }

    @FXML
    private void onAddReview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListReview.fxml"));
            Parent root = loader.load();
            ListReviewController ctrl = loader.getController();
            ctrl.init(product);

            Stage stage = new Stage();
            stage.setTitle("Recensioni prodotto");
            stage.initOwner(addReview.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception ex) {
            showError("Impossibile aprire le recensioni:\n" + ex.getMessage(), ex);
        }
    }

    @FXML
    private void addToWishList() {
        try {
            if (!ensureSizeSelectedOrWarn()) return;

            UserDAO.addInWishList(Session.getUser(), product.getProductId(),
                    product.getIdShop(), product.getSize());

            addToWishListBtn.setDisable(true);
            addToWishListBtn.setText(TXT_ADDED_TO_WISHLIST);

        } catch (SQLException e) {
            showError("Impossibile aggiungere ai preferiti:\n" + e.getMessage(), e);
        }
    }

    private void refreshForSelectedSize(String sel) throws SQLException {
        // aggiorna prezzo
        double priceSel = productDao.getPriceFor(product.getProductId(), product.getIdShop(), sel);
        product.setPrice(priceSel);
        priceLbl.setText(String.format(EUR_PRICE_FMT, priceSel));

        boolean wished = productDao.existsWish(Session.getUser(),
                product.getProductId(), product.getIdShop(), sel);
        updateWishButton(wished);

        // aggiorna disponibilità e range quantità
        updateStockAndQtyRange();
    }

    private boolean ensureSizeSelectedOrWarn() {
        String selSize = (sizeCombo != null) ? sizeCombo.getValue() : null;
        if (selSize == null || selSize.isBlank()) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Taglia mancante");
            a.setHeaderText(null);
            a.setContentText("Seleziona una taglia prima di procedere.");
            a.showAndWait();
            return false;
        }
        product.setSize(selSize);
        return true;
    }

    private void showError(String message, Throwable t) {
        Logger.getLogger(getClass().getName()).log(Level.WARNING, message, t);
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }


    @FXML
    private void onClose() {
        // chiude lo stage
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

}
