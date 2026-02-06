package org.example.control.services;

import org.example.dao.ProductDaos;
import org.example.dao.api.ProductDao;
import org.example.models.Aggregated;
import org.example.models.CartItem;
import org.example.models.CheckoutData;
import org.example.models.Key;
import org.example.models.Product;
import org.example.util.Session;

import java.math.BigDecimal;
import java.util.*;

public class CartService {
    private final ProductDao productDao = ProductDaos.create();

    public List<Product> getCartItems() {
        return Session.getCartItems();
    }

    public int getStockFor(long productId, int shopId, String size){
        return productDao.getStockFor(productId, shopId, size);
    }

    public CheckoutData buildCheckoutData() {
        List<Product> products = getCartItems();
        if (products == null || products.isEmpty()) {
            return new CheckoutData(List.of(), BigDecimal.ZERO);
        }
        Map<Key, Aggregated> aggregated = aggregateCartItems(products);
        return buildCheckoutDataFromAggregated(aggregated);
    }

    public Map<Key, Aggregated> getAggregatedCart() {
        List<Product> items = getCartItems();
        if (items.isEmpty()) return Map.of();
        return aggregateCartItems(items);
    }

    public Map<Key, Aggregated> aggregateCartItems(List<Product> products) {
        Map<Key, Aggregated> map = new LinkedHashMap<>();
        for (Product p : products) {
            Key k = new Key(p.getProductId(), p.getIdShop(), p.getSize());
            map.compute(k, (ignored, agg) -> {
                if (agg == null) return new Aggregated(p, 1);
                agg.incrementQty();
                return agg;
            });
        }
        return map;
    }

    private CheckoutData buildCheckoutDataFromAggregated(Map<Key, Aggregated> map) {
        List<CartItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Aggregated agg : map.values()) {
            Product p = agg.sample;
            int qty = agg.getQty();
            double priceDouble = p.getPrice();

            items.add(new CartItem(
                    p.getProductId(), p.getIdShop(), qty, priceDouble,
                    p.getName(), p.getImageData(), p.getSize()
            ));

            BigDecimal unit = BigDecimal.valueOf(priceDouble);
            total = total.add(unit.multiply(BigDecimal.valueOf(qty)));
        }
        return new CheckoutData(items, total);
    }

    public void changeQuantity(Product p, int delta) {
        if (delta < 0) {
            Session.removeFromCart(p);
        } else {
            Session.addToCart(Product.copyOf(p));
        }
    }

    public void removeLine(long productId, int idShop, String size) {
        Session.removeLineFromCart(productId, idShop, size);
    }

    public void clearCart() {
        Session.clearCart();
    }
}

