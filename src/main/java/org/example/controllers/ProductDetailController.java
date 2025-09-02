package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.dao.ProductDAO;
import org.example.dao.UserDAO;
import org.example.models.Product;
import org.example.util.Session;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDetailController {

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

    public void setOnAddToCartCallback(Runnable callback) {
        this.onAddToCartCallback = callback;
    }

    public void setProduct(Product p) throws SQLException {
        this.product = p;
        bigPhoto.setImage(p.getImage());
        nameLbl.setText(p.getName());
        nameShop.setText(p.getNameShop());
        priceLbl.setText(String.format(EUR_PRICE_FMT, p.getPrice()));
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        qtySpinner.setEditable(true);

        // 1) Popola taglie disponibili per (product_id, shop)
        try {
            List<String> sizes = ProductDAO.getAvailableSizes(p.getProductId(), p.getIdShop());
            sizeCombo.getItems().setAll(sizes);
            if (!sizes.isEmpty()) {
                sizeCombo.getSelectionModel().selectFirst();
                String sel = sizeCombo.getValue();
                product.setSize(sel); // tieni la taglia nel model

                // 2) Aggiorna prezzo in base alla taglia selezionata
                double priceSel = ProductDAO.getPriceFor(p.getProductId(), p.getIdShop(), sel);
                product.setPrice(priceSel);
                priceLbl.setText(String.format(EUR_PRICE_FMT, priceSel));

                // 3) Aggiorna stato bottone “Preferiti” per la taglia selezionata
                boolean alreadyWishedBySize =
                        ProductDAO.existsWish(Session.getUser(), p.getProductId(), p.getIdShop(), sel);
                updateWishButton(alreadyWishedBySize);

                updateStockAndQtyRange();
            } else {
                sizeCombo.setDisable(true); // nessuna taglia disponibile
                updateStockAndQtyRange();
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Errore caricando taglie/prezzo", ex);
        }

        // controllo se è già nella wishlist
        boolean alreadyWished = ProductDAO.existsWish(Session.getUser(),
                product.getProductId(),
                product.getIdShop());
        if (alreadyWished) {
            addToWishListBtn.setText(TXT_ADDED_TO_WISHLIST);
            addToWishListBtn.setDisable(true);
        }

        // Listener: quando cambia la taglia, aggiorna prezzo e stato preferiti
        sizeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, sel) -> {
            if (sel == null) return;
            product.setSize(sel);
            try {
                double priceSel = ProductDAO.getPriceFor(product.getProductId(), product.getIdShop(), sel);
                product.setPrice(priceSel);
                priceLbl.setText(String.format(EUR_PRICE_FMT, priceSel));

                boolean wished = ProductDAO.existsWish(Session.getUser(),
                        product.getProductId(), product.getIdShop(), sel);
                updateWishButton(wished);

                updateStockAndQtyRange();
            } catch (SQLException e) {
                logger.log(Level.WARNING, e,
                        () -> "Errore aggiornando prezzo/stato preferiti per taglia " + sel);

            }
        });

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

            stock = ProductDAO.getStockFor(product.getProductId(), product.getIdShop(), sel);

            int max = stock;

            if (max <= 0) {
                // esaurito
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
            // fallback prudente
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
            ctrl.init(product); // passa il prodotto da mostrare

            Stage stage = new Stage();
            stage.setTitle("Recensioni prodotto");
            stage.initOwner(addReview.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception ex) {
            Logger.getLogger(ProductDetailController.class.getName())
                    .log(Level.WARNING, "Errore aprendo la lista recensioni", ex);
            new Alert(Alert.AlertType.ERROR,
                    "Impossibile aprire le recensioni:\n" + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void addToWishList() {
        try {

            String selSize = (sizeCombo != null) ? sizeCombo.getValue() : null;
            if (selSize == null || selSize.isBlank()) {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("Taglia mancante");
                a.setHeaderText(null);
                a.setContentText("Seleziona una taglia prima di aggiungere ai preferiti.");
                a.showAndWait();
                return;
            }
            product.setSize(selSize);

            // 1. Inserisci in DB
            UserDAO.addInWishList(Session.getUser(), product.getProductId(), product.getIdShop(), product.getSize());

            // 2. Feedback all’utente
            addToWishListBtn.setDisable(true);
            addToWishListBtn.setText(TXT_ADDED_TO_WISHLIST);

        } catch (SQLException e) {
            // Gestione dell’errore
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText("Impossibile aggiungere alla lista dei preferiti");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

    }

    @FXML
    private void onClose() {
        // chiude lo stage
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

}
