package org.example.control.services;

import org.example.dao.ProductDaos;
import org.example.dao.api.ProductDao;
import org.example.models.Aggregated;
import org.example.models.CartItem;
import org.example.models.CheckoutData;
import org.example.models.Key;
import org.example.models.Product;

import java.math.BigDecimal;
import java.util.*;

public class CartService {
    private final ProductDao productDao = ProductDaos.create();

    public int getStockFor(long productId, int shopId, String size){
        return productDao.getStockFor(productId, shopId, size);
    }

    public CheckoutData buildCheckoutData(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return new CheckoutData(List.of(), BigDecimal.ZERO);
        }
        Map<Key, Aggregated> aggregated = aggregateCartItems(cartItems);
        return buildCheckoutDataFromAggregated(aggregated);
    }

    public Map<Key, Aggregated> getAggregatedCart(List<CartItem> cartItems) {
        if (cartItems.isEmpty()) return Map.of();
        return aggregateCartItems(cartItems);
    }

    public Map<Key, Aggregated> aggregateCartItems(List<CartItem> cartItems) {

        cartItems = new ArrayList<>(cartItems);
        cartItems.sort(Comparator
                .comparing(CartItem::getProductId)
                .thenComparingInt(CartItem::getShopId)
                .thenComparing(CartItem::getSize)
        );

        Map<Key, Aggregated> map = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            Key k = new Key(item.getProductId(), item.getShopId(), item.getSize());
            map.compute(k, (ignored, agg) -> {
                if (agg == null) return new Aggregated(item.getSampleProduct(), item.getQuantity());
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
}

