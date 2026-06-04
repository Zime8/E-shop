package org.example.dao.demo;

import org.example.dao.OrderRepository;
import org.example.dao.RepositoryException;
import org.example.dao.support.OrderValidation;
import org.example.demo.DemoData;
import org.example.models.entity.*;

import java.time.LocalDateTime;
import java.util.*;

public final class DemoOrderDAO implements OrderRepository {

    @Override
    public CreationResult placeOrderWithStockDecrement(int userId, List<CartItem> items, String address) {
        OrderValidation.validateUserId(userId);
        OrderValidation.validateItems(items);

        ensureDemoSeed();
        Map<String, Integer> need = aggregateNeed(items);
        validateDemoStock(need);

        Map<Integer, List<CartItem>> byShop = groupByShop(items);
        CreationResult res = createDemoOrders(userId, byShop);

        decrementDemoStock(need);
        return res;
    }

    @Override
    public List<Order> listOrdersModel(int userId) {
        OrderValidation.validateUserId(userId);

        DemoData.ensureLoaded();
        var src = DemoData.orders().getOrDefault(userId, Collections.emptyList());
        List<Order> out = new ArrayList<>(src.size());

        for (Order o : src) {
            Order copy = new Order(o.getId(), o.getUserId(), o.getCreatedAt(), o.getStatus());
            for (OrderLine l : o.getLines()) {
                copy.addLine(new OrderLine(
                        l.getOrderId(), l.getProductId(), l.getShopId(),
                        new OrderLine.Details(
                                l.getProductName(),
                                l.getShopName(),
                                l.getSize(),
                                l.getQuantity(),
                                l.getUnitPrice()
                        )
                ));
            }
            out.add(copy);
        }

        out.sort((a, b) -> {
            int c = b.getCreatedAt().compareTo(a.getCreatedAt());
            return (c != 0) ? c : Integer.compare(b.getId(), a.getId());
        });

        return out;
    }

    private String stockKey(long productId, int shopId, String size) {
        return DemoData.stockKey(productId, shopId, size);
    }

    private void ensureDemoSeed() {
        DemoData.ensureLoaded();
        // Inizializza stock demo se mancante
        for (Product p : DemoData.products().values()) {
            DemoData.stock().putIfAbsent(stockKey(p.productId(), p.idShop(), p.size()), 5);
        }
    }

    private Map<String, Integer> aggregateNeed(List<CartItem> items) {
        Map<String, Integer> need = new LinkedHashMap<>();
        for (CartItem it : items) {
            String key = stockKey(it.productId(), it.shopId(), it.size());
            need.merge(key, it.quantity(), Integer::sum);
        }
        return need;
    }

    private void validateDemoStock(Map<String, Integer> need) {
        for (var e : need.entrySet()) {
            int available = DemoData.stock().getOrDefault(e.getKey(), 0);
            int required = e.getValue();
            if (available < required) {
                String[] parts = e.getKey().split("\\|");
                long pid = Long.parseLong(parts[0]);
                int sid  = Integer.parseInt(parts[1]);
                String sz = parts[2];
                throw new RepositoryException("In demo, esaurito produtto =" + pid
                        + ", shop=" + sid + ", size=" + sz
                        + " (richiesto " + required + ", disponibile " + available + ")");
            }
        }
    }

    private Map<Integer, List<CartItem>> groupByShop(List<CartItem> items) {
        Map<Integer, List<CartItem>> byShop = new LinkedHashMap<>();
        for (CartItem it : items) {
            byShop.computeIfAbsent(it.shopId(), k -> new ArrayList<>()).add(it);
        }
        return byShop;
    }

    private CreationResult createDemoOrders(int userId, Map<Integer, List<CartItem>> byShop) {
        List<Integer> createdIds = new ArrayList<>();
        Map<Integer, Integer> shopToOrderId = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (var entry : byShop.entrySet()) {
            int shopId = entry.getKey();
            List<CartItem> group = entry.getValue();

            int orderId = DemoData.DEMO_ORDER_ID.incrementAndGet();
            shopToOrderId.put(shopId, orderId);
            createdIds.add(orderId);

            Order ord = new Order(orderId, userId, now, OrderStatus.IN_ELABORAZIONE);
            for (CartItem it : group) {
                String prodKey = DemoData.prodKey(it.productId(), it.shopId(), it.size());
                Product p = DemoData.products().get(prodKey);
                String productName = (p != null) ? p.name()     : ("Prodotto #" + it.productId());
                String shopName    = (p != null) ? p.nameShop() : ("Shop #" + it.shopId());

                ord.addLine(new OrderLine(
                        orderId,
                        it.productId(),
                        it.shopId(),
                        new OrderLine.Details(
                                productName,
                                shopName,
                                it.size(),
                                it.quantity(),
                                it.unitPrice()
                        )
                ));
            }
            DemoData.orders()
                    .computeIfAbsent(userId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(ord);
        }
        return new CreationResult(createdIds, shopToOrderId);
    }

    private void decrementDemoStock(Map<String, Integer> need) {
        for (var e : need.entrySet()) {
            int available = DemoData.stock().getOrDefault(e.getKey(), 0);
            DemoData.stock().put(e.getKey(), available - e.getValue());
        }
    }
}