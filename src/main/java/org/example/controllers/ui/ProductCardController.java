package org.example.controllers.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.example.controllers.app.BuyProductController;
import org.example.models.Product;
import org.example.util.ImageUtils;
import org.example.util.Navigator;

public class ProductCardController {

    @FXML private ImageView photo;
    @FXML private Label nameLbl ;
    @FXML private Label priceLbl;
    @FXML private Label nameShopLbl;

    private Product product;

    private BuyProductController appController;

    public void setController(Object appController) {
        this.appController = (BuyProductController) appController;
    }

    @SuppressWarnings("unused")
    public void loadData(Object dataObj) {
        if (dataObj instanceof Product p) {
            setProduct(p);
        }
    }

    // Setta i prodotti nella schermata home
    public void setProduct(Product p) {
        this.product = p;
        if(p == null) return;

        ImageUtils.setImage(photo, p.getImageData());
        nameLbl.setText(p.getName());
        nameShopLbl.setText(p.getNameShop());
        priceLbl.setText(String.format("€ %.2f", p.getPrice()));
    }

    public void setOnCartUpdate(Runnable callback) {
        if (appController != null) {
            appController.setOnAddToCartCallback(() -> {
                Platform.runLater(callback);  // Solo thread-safety
            });
        }
    }

    // Apri il dettaglio del prodotto se cliccato
    @FXML private void onCardClicked() {
        Navigator.openModal("/fxml/ProductDetail.fxml", product,
                appController, appController.getOnAddToCartCallback());
    }
}
