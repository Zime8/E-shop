package org.example.boundary;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.example.control.BuyProductController;
import org.example.control.ReviewAppController;
import org.example.models.dto.ProductDto;
import org.example.util.ImageUtils;
import org.example.util.Navigator;

public class ProductCardController {

    @FXML private ImageView photo;
    @FXML private Label nameLbl ;
    @FXML private Label priceLbl;
    @FXML private Label nameShopLbl;

    private ProductDto product;
    private Runnable onCartUpdate;

    private BuyProductController appController;
    private ReviewAppController reviewAppController;
    private Navigator navigator;

    public void setAppController(BuyProductController appController) {
        this.appController = appController;
    }

    public void setReviewAppController(ReviewAppController reviewAppController) {
        this.reviewAppController = reviewAppController;
    }

    public void setNavigator(Navigator navigator){
        this.navigator = navigator;
    }

    @SuppressWarnings("unused")
    public void loadData(Object dataObj) {
        if (dataObj instanceof ProductDto p) {
            setProduct(p);
        }
    }

    // Setta i prodotti nella schermata home
    public void setProduct(ProductDto p) {
        this.product = p;
        if(p == null) return;

        ImageUtils.setImage(photo, p.imageData());
        nameLbl.setText(p.name());
        nameShopLbl.setText(p.nameShop());
        priceLbl.setText(String.format("€ %.2f", p.unitPrice()));
    }

    public void setOnCartUpdate(Runnable callback) {
        this.onCartUpdate = callback;
    }

    // Apri il dettaglio del prodotto se cliccato
    @FXML
    private void onCardClicked() {
        if (product == null || appController == null || navigator == null) {
            return;
        }

        navigator.openTransparentModal("/fxml/ProductDetail.fxml", (ProductDetailController controller) -> {
            controller.setAppController(appController);
            controller.setReviewAppController(reviewAppController);
            controller.setNavigator(navigator);
            controller.setOnCartUpdate(onCartUpdate);
            controller.setProduct(product);
        });
    }
}
