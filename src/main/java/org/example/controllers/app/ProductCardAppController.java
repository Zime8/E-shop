package org.example.controllers.app;

import org.example.models.Product;

public class ProductCardAppController {
    private Product currentProduct;
    private Runnable onAddToCartCallback;

    public void setProduct(Product product, Runnable onAddToCartCallback) {
        this.currentProduct = product;
        this.onAddToCartCallback = onAddToCartCallback;
    }

    public void setOnAddToCartCallback(Runnable callback) {
        this.onAddToCartCallback = callback;
    }

    public Product getCurrentProduct() { return currentProduct; }
    public Runnable getOnAddToCartCallback() { return onAddToCartCallback; }
}

