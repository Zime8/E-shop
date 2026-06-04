package org.example.demo;

import org.example.models.dto.Card;
import org.example.models.entity.Order;
import org.example.models.entity.Product;
import org.example.models.entity.Review;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class DemoData {

    private DemoData(){}

    private static final Map<String, User> USERS = new ConcurrentHashMap<>();
    private static final Map<String, List<Product>> WISHLISTS = new ConcurrentHashMap<>();
    private static final Map<String, Product> PRODUCTS = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Card>> SAVED_CARDS = new ConcurrentHashMap<>();
    private static final Map<String, List<Review>> REVIEWS = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Order>> ORDERS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> STOCK = new ConcurrentHashMap<>();

    public static Map<String, User> users() { return USERS; }
    public static Map<String, List<Product>> wishlists() { return WISHLISTS; }
    public static Map<String, Product> products() { return PRODUCTS; }
    public static Map<Integer, List<Card>> savedCards() { return SAVED_CARDS; }
    public static Map<String, List<Review>> reviews() { return REVIEWS; }
    public static Map<Integer, List<Order>> orders() { return ORDERS; }
    public static Map<String, Integer> stock() { return STOCK; }

    public record User(Integer id, String username, String passHash, String role, String email, String phone) {}

    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    public static final AtomicInteger DEMO_CARD_ID = new AtomicInteger(1);
    public static final AtomicInteger DEMO_ORDER_ID = new AtomicInteger(1000);
    public static final AtomicInteger NEXT_DEMO_USER_ID = new AtomicInteger(-1000);

    public static void ensureLoaded() {
        if (!INIT.compareAndSet(false, true)) return;

        USERS.putIfAbsent("seed-demo", new User(0, "seed-demo", null, "utente", "demo@example.com", "000"));

        // Qualche prodotto di esempio
        PRODUCTS.put(prodKey(1001, 1, "42"), makeProduct(1001, "Scarpa Demo Run", "Running", "BrandX", "Scarpe", 59.90, "42"));
        PRODUCTS.put(prodKey(1002, 1, "M"),  makeProduct(1002, "Maglia Demo", "Calcio", "BrandY", "Maglie", 39.90, "M"));
        PRODUCTS.put(prodKey(1003, 1, "43"), makeProduct(1003, "Nike Air Zoom Sprint", "Running", "Nike", "Scarpe", 89.90, "43"));
        PRODUCTS.put(prodKey(1004, 1, "44"), makeProduct(1004, "Adidas Ultraboost Demo", "Running", "Adidas", "Scarpe", 119.90, "44"));
        PRODUCTS.put(prodKey(1005, 2, "M"),  makeProduct(1005, "Puma Training Tee", "Fitness", "Puma", "Maglie", 29.90, "M"));
        PRODUCTS.put(prodKey(1006, 2, "L"),  makeProduct(1006, "Under Armour HeatGear", "Fitness", "Under Armour", "Maglie", 34.90, "L"));
        PRODUCTS.put(prodKey(1007, 3, "5"),  makeProduct(1007, "Wilson Tour Tennis Balls", "Tennis", "Wilson", "Accessori", 12.90, "5"));
        PRODUCTS.put(prodKey(1008, 3, "unique"), makeProduct(1008, "Babolat Pure Drive Bag", "Tennis", "Babolat", "Borse", 64.90, "unique"));
        PRODUCTS.put(prodKey(1009, 4, "L"),  makeProduct(1009, "Joma Match Jersey", "Calcio", "Joma", "Maglie", 24.90, "L"));
        PRODUCTS.put(prodKey(1010, 4, "42"), makeProduct(1010, "Mizuno Morelia Club", "Calcio", "Mizuno", "Scarpe", 79.90, "42"));
        PRODUCTS.put(prodKey(1011, 5, "unique"), makeProduct(1011, "Speedo Swim Goggles Fast", "Nuoto", "Speedo", "Accessori", 19.90, "unique"));
        PRODUCTS.put(prodKey(1012, 5, "M"),  makeProduct(1012, "Arena Swim Jammer", "Nuoto", "Arena", "Costumi", 39.90, "M"));

        for (var p : PRODUCTS.values()) {
            STOCK.putIfAbsent(stockKey(p.productId(), p.idShop(), p.size()), 5);
        }
    }

    public static String prodKey(long productId, int idShop, String size) {
        return productId + "|" + idShop + "|" + (size == null ? "" : size);
    }

    // Chiave per le recensioni per (productId, shopId)
    public static String reviewKey(long productId, int idShop) {
        return productId + "|" + idShop;
    }

    public static String stockKey(long productId, int shopId, String size) {
        return productId + "|" + shopId + "|" + (size == null ? "" : size);
    }

    // Helper per ottenere/creare la lista recensioni in demo
    public static List<Review> reviewsOf(long productId, int idShop) {
        String key = reviewKey(productId, idShop);
        return REVIEWS.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
    }

    public static void clearUserDemoReviews(String username) {
        for (var list : REVIEWS.values()) {
            list.removeIf(r ->
                    r.getUsername() != null && r.getUsername().equalsIgnoreCase(username)
            );
        }
    }

    private static Product makeProduct(int id, String name, String sport, String brand, String category,
                                       double price, String size) {
        return new Product(
                id,
                1,
                name,
                sport,
                brand,
                category,
                "Negozio Demo",
                java.math.BigDecimal.valueOf(price),
                size,
                null,
                java.time.LocalDateTime.now()
        );
    }
}
