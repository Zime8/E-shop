package org.example.controllers.app;

import org.example.models.OrderSummaryView;
import org.example.models.OrderLineView;
import org.example.control.services.PurchaseHistoryService;

import java.util.List;
import java.util.function.Consumer;

public class PurchaseHistoryAppController {
    private final PurchaseHistoryService service = new PurchaseHistoryService();

    public void loadOrders(Consumer<List<OrderSummaryView>> onOrdersLoaded, Consumer<List<OrderLineView>> onItemsUpdated) {
        service.loadOrders(onOrdersLoaded, onItemsUpdated);
    }

    public void loadItemsForOrder(int orderId, Consumer<List<OrderLineView>> onItemsLoaded) {
        service.loadItemsForOrder(orderId, onItemsLoaded);
    }
}


