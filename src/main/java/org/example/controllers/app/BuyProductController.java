package org.example.controllers.app;

import org.example.controllers.ui.ProductCardController;
import org.example.services.HomeService;
import org.example.services.CartService;
import org.example.services.PaymentSelectionService;
import org.example.dao.ProductDaos;
import org.example.dao.ShopDAO;
import org.example.dao.api.ProductDao;
import org.example.gateway.FakePaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.*;
import org.example.util.Session;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuyProductController {
    private final HomeService homeService = new HomeService();
    private final CartService cartService = new CartService();
    private final PaymentSelectionService paymentService = new PaymentSelectionService(new FakePaymentGateway(1000L, 0.10));
    private final ProductDao productDao = ProductDaos.create();

    private static final Logger logger = Logger.getLogger(BuyProductController.class.getName());

    // Schermata home: ricerca, filtri e setup product card
    public List<Product> searchByName(String query) {
        return homeService.searchByName(query);
    }
    public List<Product> findLatest(int limit) {
        return homeService.findLatest(limit);
    }
    public List<Product> searchByFilters(FilterCriteria criteria) {
        return homeService.searchByFilters(criteria);
    }
    public FilterCriteria createFilterCriteria(String sport, String brand, String shop, String category, double minPrice, double maxPrice) {
        return new FilterCriteria(sport, brand, shop, category, minPrice, maxPrice);
    }
    public FilterCriteria resetFilters() {
        return FilterCriteria.defaults();
    }

    public void createProductCard(ProductCardController uiController, Product product, Runnable callback) {
        uiController.setController(this);
        uiController.setProduct(product);
        uiController.setOnCartUpdate(callback);
    }

    // Schermata carta prodotto: gestione prodotto corrente
    private Product currentProduct;
    private Runnable onAddToCartCallback;

    public void setProduct(Product product, Runnable onAddToCartCallback) {
        this.currentProduct = product;
        this.onAddToCartCallback = onAddToCartCallback;
    }
    public Product getCurrentProduct() {
        return currentProduct;
    }
    public Runnable getOnAddToCartCallback(){
        return onAddToCartCallback;
    }
    public void setOnAddToCartCallback(Runnable  callback) {
        this.onAddToCartCallback = callback;
    }

    // Schermata dettaglio prodotto: dettagli prodotto, wishlist, stock, cart add
    public Shop getShopInfo(int shopId) {
        return ShopDAO.getById(shopId);
    }
    public List<String> getAvailableSizes(long productId, int shopId) {
        return productDao.getAvailableSizes(productId, shopId);
    }
    public double getPriceFor(long productId, int shopId, String size) {
        return productDao.getPriceFor(productId, shopId, size);
    }
    public Integer getStockFor(long productId, int shopId, String size) {
        return productDao.getStockFor(productId, shopId, size);
    }
    public void addToCart(CartItem item, int quantity) {
        CartItem qtyItem = item.withQuantity(quantity);
        Session.addToCart(qtyItem);
        if (onAddToCartCallback != null) {
            onAddToCartCallback.run(); // Trigger refresh badge/home
        }
    }
    public void addToCart(CartItem item) {
        addToCart(item, item.getQuantity());
    }
    public boolean isValidQuantity(int quantity, int maxStock) {
        return quantity > 0 && quantity <= maxStock;
    }

    // SChermata gestione carrello
    public List<CartItem> getCartItems() {
        return Session.getCartItems();
    }
    public int getCartCount() {
        return Session.getCartItems().size();
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

    // Schermata dettaglio ordine
    public record ItemView(String productName, String size, int quantity,
                           BigDecimal unitPrice, Object imageObj, BigDecimal subtotal) {}

    public record DisplayData(List<ItemView> rows, BigDecimal total) {}

    public DisplayData processItemsForDisplay(List<CartItem> items, BigDecimal total) {
        var rows = new ArrayList<ItemView>();
        BigDecimal safeTotal = total != null ? total : BigDecimal.ZERO;
        if (items != null) {
            for (CartItem it : items) {
                try {
                    BigDecimal unit = it.getUnitPrice() != null ?
                            BigDecimal.valueOf(it.getUnitPrice()) : BigDecimal.ZERO;
                    BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(it.getQuantity()));
                    rows.add(new ItemView(
                            it.getProductName(), it.getSize(), it.getQuantity(),
                            unit, it.getProductImage(), subtotal
                    ));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Errore caricamento prodotto: ", e);
                }
            }
        }
        return new DisplayData(List.copyOf(rows), safeTotal);
    }

    // Schermata del pagamento
    private Integer userId;
    public void loadUserData(int userId) {
        this.userId = userId;
    }
    public List<CardViewModel> loadSavedCards() {
        return paymentService.loadSavedCards(userId);
    }
    public PaymentResult confirmPayment(Card card, String cvv, String address,
                                        List<CartItem> items, BigDecimal total) {
        return paymentService.confirmPayment(card, cvv, address, items, total, userId);
    }
    public String getLastOrderIds() {
        return paymentService.getLastOrderIds();
    }

}
