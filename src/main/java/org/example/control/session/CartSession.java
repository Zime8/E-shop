package org.example.control.session;

import org.example.models.entity.CartItem;

import java.util.List;

public interface CartSession {
    List<CartItem> getCartItems();
    void setCartItems(List<CartItem> items);
    void addToCart(CartItem item);
    void removeFromCart(CartItem item);
    void removeLineFromCart(long productId, int shopId, String size);
    void clearCart();
}
