package org.example.control.services;

import org.example.dao.OrderRepository;
import org.example.models.entity.OrderLine;
import org.example.models.entity.Order;
import org.example.models.dto.OrderLineView;
import org.example.models.dto.OrderSummaryView;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PurchaseHistoryService {

    private static final Logger logger = Logger.getLogger(PurchaseHistoryService.class.getName());
    private final OrderRepository orderRepository;
    private final ExecutorService executor;

    public PurchaseHistoryService(OrderRepository orderRepository, ExecutorService executor) {
        this.orderRepository = orderRepository;
        this.executor = executor;
    }

    public void loadOrders(Integer userId,
                           Consumer<ProcessResult> callback) {
        if (userId == null) {
            callback.accept(new ProcessResult(List.of(), Map.of()));
            return;
        }

        executor.submit(() -> {
            try {
                List<Order> fullOrders = orderRepository.listOrdersModel(userId);
                ProcessResult result = processOrders(fullOrders);

                callback.accept(result);
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Errore caricamento ordini per userId=" + userId, e);
                callback.accept(new ProcessResult(List.of(), Map.of()));
            }
        });
    }

    public List<OrderLineView> loadItemsForOrder(int orderId, Map<Integer, List<OrderLineView>> orderCache) {
        return orderCache.getOrDefault(orderId, List.of());
    }

    private ProcessResult processOrders(List<Order> orders) {
        List<OrderSummaryView> summaries = new java.util.ArrayList<>();
        Map<Integer, List<OrderLineView>> newCache = new java.util.HashMap<>();

        for (Order o : orders) {
            List<OrderLine> lines = o.getLines();

            List<OrderLineView> viewLines = lines.stream()
                    .map(this::modelLineToView)
                    .toList();

            newCache.put(o.getId(), viewLines);

            BigDecimal total = lines.stream()
                    .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            summaries.add(new OrderSummaryView(
                    o.getId(),
                    Timestamp.valueOf(o.getCreatedAt()),
                    o.getStatus().toDb(),
                    total
            ));
        }

        return new ProcessResult(summaries, newCache);
    }

    private OrderLineView modelLineToView(OrderLine l) {
        return new OrderLineView(
                l.getOrderId(),
                l.getProductId(),
                l.getShopId(),
                l.getProductName(),
                l.getShopName(),
                l.getSize(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity()))
        );
    }

    public record ProcessResult(List<OrderSummaryView> summaries, Map<Integer, List<OrderLineView>> cache) {}
}
