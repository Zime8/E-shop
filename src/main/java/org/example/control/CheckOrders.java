package org.example.control;

import javafx.application.Platform;
import org.example.control.session.UserContext;
import org.example.models.dto.OrderSummaryView;
import org.example.models.dto.OrderLineView;
import org.example.control.services.PurchaseHistoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CheckOrders {
    private final PurchaseHistoryService purchaseHistoryService;
    private final UserContext userContext;
    private final Map<Integer, List<OrderLineView>> orderCache = new HashMap<>();

    public CheckOrders(PurchaseHistoryService purchaseHistoryService, UserContext userContext) {
        this.purchaseHistoryService = purchaseHistoryService;
        this.userContext = userContext;
    }

    public void loadOrders(Consumer<List<OrderSummaryView>> onOrdersLoaded,
                           Consumer<List<OrderLineView>> onItemsUpdated) {
        Integer userId = userContext.getCurrentUserId();

        purchaseHistoryService.loadOrders(userId, result -> Platform.runLater(() -> {
            orderCache.clear();
            orderCache.putAll(result.cache());
            onOrdersLoaded.accept(result.summaries());
            onItemsUpdated.accept(List.of());
        }));
    }

    public void loadItemsForOrder(int orderId, Consumer<List<OrderLineView>> onItemsLoaded) {
        List<OrderLineView> items = purchaseHistoryService.loadItemsForOrder(orderId, orderCache);
        onItemsLoaded.accept(items);
    }
}


