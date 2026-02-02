package org.example.controllers.app;

import org.example.controllers.ui.ProductDetailController;
import org.example.models.Product;

public class ProductCardAppController {
    private Product currentProduct;
    private Runnable onAddToCartCallback;

    public void setProduct(Product product, Runnable onAddToCartCallback) {
        this.currentProduct = product;
        this.onAddToCartCallback = onAddToCartCallback;
    }

    // ✅ BCE: AppController SETUPPA solo dati, NO FXML/Stage
    public void setupProductDetail(ProductDetailController controller) {
        controller.setProduct(currentProduct);
        controller.setOnCartUpdate(onAddToCartCallback);
    }
}
