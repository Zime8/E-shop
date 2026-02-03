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

    public void setupProductDetail(ProductDetailController controller) {
        controller.setProduct(currentProduct);
        controller.setOnCartUpdate(onAddToCartCallback);
    }
}
