package org.example.controllers.app;

import org.example.control.services.CartService;
import org.example.models.*;
import org.example.util.Session;

import java.util.*;

public class CartAppController {
    private final CartService cartService;

    public CartAppController() {
        this.cartService = new CartService();
    }

    public List<CartItem> getCartItems() {
        return Session.getCartItems();
    }

    public int getStockFor(long productId, int shopId, String size) {
        return cartService.getStockFor(productId, shopId, size);
    }

    public CheckoutData buildCheckoutData() {
        return cartService.buildCheckoutData(Session.getCartItems());
    }

    public Map<Key, Aggregated> getAggregatedCart() {
        return cartService.getAggregatedCart(Session.getCartItems());
    }

    public void changeQuantity(long productId, int shopId, String size, int delta) {
        List<CartItem> cart = new ArrayList<>(Session.getCartItems());
        Optional<CartItem> existing = cart.stream()
                .filter(i -> i.getProductId() == productId && i.getShopId() == shopId
                        && Objects.equals(i.getSize(), size))
                .findFirst();

        if (existing.isEmpty()) return;

        CartItem current = existing.get();
        int newQty = current.getQuantity() + delta;

        Session.removeLineFromCart(productId, shopId, size);

        if (newQty > 0) {
            CartItem updated = current.withQuantity(newQty);
            Session.addToCart(updated);
        }
    }

    public void changeQuantity(CartItem item, int delta) {
        changeQuantity(item.getProductId(), item.getShopId(), item.getSize(), delta);
    }

    public void removeLine(long productId, int shopId, String size) {
        Session.removeLineFromCart(productId, shopId, size);
    }

    public void clearCart() {
        Session.clearCart();
    }
}

