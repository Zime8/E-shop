    package org.example.util;

    import org.example.models.Product;

    import java.util.*;
    import java.util.concurrent.ConcurrentHashMap;

    public final class Session {
        private Session() {}

        // userId corrente
        private static final ThreadLocal<Integer> currentUserId = new ThreadLocal<>();

        // userId → stato privato utente
        private static final Map<Integer, UserSession> sessions = new ConcurrentHashMap<>();

        // Stato privato per utente
        private static class UserSession {
            String currentUser;
            boolean demo;
            final List<Product> cartItems = new ArrayList<>();

            UserSession(String user) {
                this.currentUser = user;
            }
        }

        // Get session per user corrente
        private static UserSession getCurrentSession() {
            Integer userId = currentUserId.get();
            if (userId == null) return null;
            return sessions.computeIfAbsent(userId, k -> new UserSession(null));
        }

        // UTENTE
        public static String getUser() {
            UserSession s = getCurrentSession();
            return s != null ? s.currentUser : null;
        }

        public static void setUser(String user) {
            UserSession s = getCurrentSession();
            if (s != null) s.currentUser = user;
        }

        public static Integer getUserId() {
            return currentUserId.get();
        }

        public static void setUserId(Integer userId) {
            currentUserId.set(userId);
        }

        public static void login(int userId, String username) {
            setUserId(userId);
            UserSession s = getCurrentSession();
            if(s == null) return;
            s.currentUser = username;
        }

        public static void logout() {
            Integer userId = getUserId();
            if (userId != null) {
                sessions.remove(userId);
                currentUserId.remove();
            }
        }

        // CARRELLO (per user corrente)
        public static List<Product> getCartItems() {
            UserSession s = getCurrentSession();
            return s != null ? List.copyOf(s.cartItems) : List.of();
        }

        public static void addToCart(Product item) {
            UserSession s = getCurrentSession();
            if (s == null) return;
            s.cartItems.add(item);
        }

        public static void removeFromCart(Product item) {
            UserSession s = getCurrentSession();
            if (s == null) return;
            s.cartItems.remove(item);
        }

        public static void clearCart() {
            UserSession s = getCurrentSession();
            if (s != null) s.cartItems.clear();
        }

        public static void removeLineFromCart(long productId, int shopId, String size) {
            UserSession s = getCurrentSession();
            if (s == null) return;
            s.cartItems.removeIf(p ->
                    p.getProductId() == productId &&
                            p.getIdShop() == shopId &&
                            Objects.equals(p.getSize(), size)
            );
        }

        // DEMO
        public static boolean isDemo() {
            UserSession s = getCurrentSession();
            return s != null && s.demo;
        }

        public static void setDemo(boolean demo) {
            UserSession s = getCurrentSession();
            if (s != null) s.demo = demo;
        }
    }
