package org.example.util;

import org.example.models.Product;

import java.util.ArrayList;
import java.util.List;

public final class Session {

    private Session() {}

    private static String currentUser;
    private static Integer currentUserId;
    private static boolean demo;

    private static final List<Product> cartItems = new ArrayList<>();
    private static final List<Product> wishListItems = new ArrayList<>();

    // Utente
    public static String getUser() { return currentUser; }
    public static void setUser(String user) { currentUser = user; }
    public static Integer getUserId() { return currentUserId; }
    public static void setUserId(Integer userId) { currentUserId = userId; }

    public static void clear() {
        currentUser = null;
        currentUserId = null;
        cartItems.clear();
        wishListItems.clear();
    }

    public static boolean isDemo() {
        return demo;
    }

    public static void setDemo(boolean demo) {
        Session.demo = demo;
    }

    // Carrello
    public static List<Product> getCartItems() { return cartItems; }
    public static void addToCart(Product item) { cartItems.add(item); }
    public static void removeFromCart(Product item) { cartItems.remove(item); }
    public static void clearCart() { cartItems.clear(); }

    // Wishlist
    public static List<Product> getWishListItems() { return wishListItems; }
    public static void addToWishList(Product item) { wishListItems.add(item); }
    public static void removeFromWishList(Product item) { wishListItems.remove(item); }
}
