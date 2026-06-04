package org.example.control.session;

import org.example.models.entity.CartItem;
import org.example.util.Session;

import java.util.List;

public class SessionCartSession implements CartSession {

    private final Session session;

    public SessionCartSession(Session session) {
        this.session = session;
    }

    @Override
    public List<CartItem> getCartItems() {
        return session.getCartItems();
    }

    @Override
    public void setCartItems(List<CartItem> items) {
        session.setCartItems(items);
    }

    @Override
    public void addToCart(CartItem item) {
        session.addToCart(item);
    }

    @Override
    public void removeFromCart(CartItem item) {
        session.removeFromCart(item);
    }

    @Override
    public void removeLineFromCart(long productId, int shopId, String size) {
        session.removeLineFromCart(productId, shopId, size);
    }

    @Override
    public void clearCart() {
        session.clearCart();
    }
}
