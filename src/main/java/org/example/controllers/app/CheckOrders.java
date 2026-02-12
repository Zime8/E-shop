package org.example.controllers.app;

import org.example.models.OrderSummaryView;
import org.example.models.OrderLineView;
import org.example.services.PurchaseHistoryService;
import org.example.util.Session;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CheckOrders {
    private final PurchaseHistoryService service = new PurchaseHistoryService();
    private final Map<Integer, List<OrderLineView>> orderCache = new java.util.HashMap<>();

    public void loadOrders(Consumer<List<OrderSummaryView>> onOrdersLoaded, Consumer<List<OrderLineView>> onItemsUpdated) {
        service.loadOrders(Session.getUserId(),
                result -> {
                    // Aggiorna cache locale
                    orderCache.clear();
                    orderCache.putAll(result.cache());
                    onOrdersLoaded.accept(result.summaries());
                },
                onItemsUpdated);
    }

    public void loadItemsForOrder(int orderId, Consumer<List<OrderLineView>> onItemsLoaded) {
        List<OrderLineView> items = service.loadItemsForOrder(orderId, orderCache);
        onItemsLoaded.accept(items);
    }
}


