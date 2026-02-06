package org.example.controllers.app;

import org.example.control.services.CartService;
import org.example.models.Aggregated;
import org.example.models.Key;
import org.example.models.CheckoutData;
import org.example.models.Product;

import java.util.List;
import java.util.Map;

public class CartAppController {
    private final CartService cartService;

    public CartAppController() {
        this.cartService = new CartService();
    }

    public List<Product> getCartItems() {
        return cartService.getCartItems();
    }

    public int getStockFor(long productId, int shopId, String size) {
        return cartService.getStockFor(productId, shopId, size);
    }

    public CheckoutData buildCheckoutData() {
        return cartService.buildCheckoutData();
    }

    public Map<Key, Aggregated> getAggregatedCart() {
        return cartService.getAggregatedCart();
    }

    public void changeQuantity(Product p, int delta) {
        cartService.changeQuantity(p, delta);
    }

    public void removeLine(long productId, int idShop, String size) {
        cartService.removeLine(productId, idShop, size);
    }

    public void clearCart() {
        cartService.clearCart();
    }
}

