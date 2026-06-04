package org.example.control.services;

import org.example.control.session.CartSession;
import org.example.dao.ProductRepository;
import org.example.models.dto.*;
import org.example.models.entity.CartItem;
import org.example.models.entity.Product;

import java.math.BigDecimal;
import java.util.*;

public class CartService {

    private final CartSession cartSession;
    private final ProductRepository productDao;

    public CartService(CartSession cartSession, ProductRepository productDao){
        this.cartSession = cartSession;
        this.productDao = productDao;
    }

    public CheckoutData buildCheckoutData(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return new CheckoutData(List.of(), BigDecimal.ZERO);
        }
        Map<Key, Aggregated> aggregated = aggregateCartItems(cartItems);
        return buildCheckoutDataFromAggregated(aggregated);
    }

    public CheckoutData buildCheckoutData() {
        return buildCheckoutData(cartSession.getCartItems());
    }

    public Map<Key, Aggregated> aggregateCartItems(List<CartItem> cartItems) {

        cartItems = new ArrayList<>(cartItems);
        cartItems.sort(Comparator
                .comparing(CartItem::productId)
                .thenComparingInt(CartItem::shopId)
                .thenComparing(item -> item.size() == null ? "" : item.size())
        );

        Map<Key, Aggregated> map = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            Key k = new Key(item.productId(), item.shopId(), item.size());
            map.compute(k, (ignored, agg) -> {
                if (agg == null) return new Aggregated(item, item.quantity());
                return agg.withQty(agg.qty() + item.quantity());
            });
        }
        return map;
    }

    private CheckoutData buildCheckoutDataFromAggregated(Map<Key, Aggregated> map) {
        List<CartItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Aggregated agg : map.values()) {
            CartItem item = agg.sample().withQuantity(agg.qty());
            items.add(item);
            total = total.add(agg.subtotal());
        }

        return new CheckoutData(items, total);
    }

    public void addWishlistItemToCart(WishlistItemDto p) {
        CartItem item = new CartItem(
                p.productId(),
                p.shopId(),
                1,
                p.price(),
                p.name(),
                p.imageData(),
                p.size()
        );
        cartSession.addToCart(item);
    }

    public void addToCart(AddToCartRequest request) {
        CartItem item = new CartItem(
                request.productId(),
                request.shopId(),
                request.quantity(),
                request.unitPrice(),
                request.productName(),
                request.imageData(),
                request.size()
        );
        cartSession.addToCart(item);
    }

    public int getCartCount() {
        return cartSession.getCartItems().stream()
                .mapToInt(CartItem::quantity)
                .sum();
    }

    public void removeLine(long productId, int shopId, String size) {
        cartSession.removeLineFromCart(productId, shopId, size);
    }

    public void clearCart() {
        cartSession.clearCart();
    }

    public void changeQuantity(long productId, int shopId, String size, int delta) {

        List<CartItem> items = new ArrayList<>(cartSession.getCartItems());

        for (int i = 0; i < items.size(); i++) {
            CartItem current = items.get(i);

            if (current.productId() == productId
                    && current.shopId() == shopId
                    && Objects.equals(current.size(), size)) {

                int newQty = current.quantity() + delta;

                if (newQty <= 0) {
                    items.remove(i);
                } else {
                    items.set(i, current.withQuantity(newQty));
                }

                cartSession.setCartItems(items);
                return;
            }
        }
    }

    public DisplayData buildDisplayData() {
        List<CartItem> items = cartSession.getCartItems();
        BigDecimal total = buildCheckoutData(items).total();

        var rows = new ArrayList<ItemView>();
        for (CartItem it : items) {
            BigDecimal unit = it.unitPrice() != null ? it.unitPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(it.quantity()));
            rows.add(new ItemView(
                    it.productName(),
                    it.size(),
                    it.quantity(),
                    unit,
                    it.productImage(),
                    subtotal
            ));
        }
        return new DisplayData(List.copyOf(rows), total);
    }

    public List<CartRowData> loadCartRows() {
        return aggregateCartItems(getCartItems()).values().stream()
                .map(agg -> {
                    CartItem item = agg.sample();
                    Optional<Product> optP = productDao.findById(item.productId());
                    Product p = optP.orElse(null);
                    if (p == null) return null;

                    int stock = getStockFor(item.productId(), item.shopId(), item.size());

                    ProductDto dto = toDto(p, item, stock);
                    return new CartRowData(dto, agg, stock, agg.qty() >= stock);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<CartItem> getCartItems() {
        return cartSession.getCartItems();
    }

    private ProductDto toDto(Product p, CartItem item, int stock) {
        return new ProductDto(
                item.productId(),
                item.shopId(),
                p.name(),
                p.nameShop(),
                p.sport(),
                p.price().doubleValue(),
                p.imageData(),
                stock,
                item.size()
        );
    }

    public Integer getStockFor(long productId, int shopId, String size) {
        return productDao.getStockFor(productId, shopId, size);
    }
}

