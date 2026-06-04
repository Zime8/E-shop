package org.example.util;

import org.example.models.entity.CartItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Session {

    private Integer currentUserId;
    private String currentUser;
    private boolean demo;
    private final List<CartItem> cartItems = new ArrayList<>();

    public Integer getUserId() {
        return currentUserId;
    }

    public String getUser() {
        return currentUser;
    }

    public void setUser(String user) {
        this.currentUser = user;
    }

    public void login(int userId, String username) {
        this.currentUserId = userId;
        this.currentUser = username;
    }

    public void logout() {
        this.currentUserId = null;
        this.currentUser = null;
        this.demo = false;
        this.cartItems.clear();
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public List<CartItem> getCartItems() {
        return List.copyOf(cartItems);
    }

    public void setCartItems(List<CartItem> items) {
        cartItems.clear();
        if (items != null) {
            cartItems.addAll(items);
        }
    }

    public void addToCart(CartItem item) {
        if (item == null) return;

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem existing = cartItems.get(i);
            if (sameLine(existing, item.productId(), item.shopId(), item.size())) {
                cartItems.set(i, existing.withQuantity(existing.quantity() + item.quantity()));
                return;
            }
        }

        cartItems.add(item);
    }

    private boolean sameLine(CartItem item, long productId, int shopId, String size) {
        return item.productId() == productId
                && item.shopId() == shopId
                && Objects.equals(item.size(), size);
    }

    public void removeFromCart(CartItem item) {
        cartItems.remove(item);
    }

    public void clearCart() {
        cartItems.clear();
    }

    public void removeLineFromCart(long productId, int shopId, String size) {
        cartItems.removeIf(p ->
                p.productId() == productId &&
                        p.shopId() == shopId &&
                        Objects.equals(p.size(), size)
        );
    }
}