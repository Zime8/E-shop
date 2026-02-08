package org.example.controllers.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.controllers.app.ProductCardAppController;
import org.example.controllers.app.ProductDetailAppController;
import org.example.models.Product;
import org.example.util.ImageUtils;
import org.example.util.Navigator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductCardController {

    private static final Logger logger = Logger.getLogger(ProductCardController.class.getName());

    @FXML private ImageView photo;
    @FXML private Label nameLbl ;
    @FXML private Label priceLbl;
    @FXML private Label nameShopLbl;

    private ProductCardAppController appController;

    public void setController(Object appController) {
        this.appController = (ProductCardAppController) appController;
    }

    @SuppressWarnings("unused")
    public void loadData(Object dataObj) {
        if (dataObj instanceof Product p) {
            setProduct(p);
        }
    }

    // Setta i prodotti nella schermata home
    public void setProduct(Product p) {
        if(p == null) return;

        ImageUtils.setImage(photo, p.getImageData());
        nameLbl.setText(p.getName());
        nameShopLbl.setText(p.getNameShop());
        priceLbl.setText(String.format("€ %.2f", p.getPrice()));

        appController.setProduct(p, appController.getOnAddToCartCallback());
    }

    public void setOnCartUpdate(Runnable callback) {
        if (appController != null) {
            appController.setOnAddToCartCallback(callback);
        }
    }

    // Apri il dettaglio del prodotto se cliccato
    @FXML private void onCardClicked() {
        Navigator.openModal("/fxml/ProductDetail.fxml", appController.getCurrentProduct(),
                appController.getOnAddToCartCallback());
    }
}
