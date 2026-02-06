package org.example.control.services;

import org.example.dao.OrderDAO;
import org.example.models.OrderLine;
import org.example.models.Order;
import org.example.models.OrderLineView;
import org.example.models.OrderSummaryView;
import org.example.util.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PurchaseHistoryService {

    private static final Logger logger = Logger.getLogger(PurchaseHistoryService.class.getName());

    // Cache per i dettagli degli ordini
    private Map<Integer, List<OrderLineView>> orderCache = new java.util.HashMap<>();

    public void loadOrders(Consumer<List<OrderSummaryView>> onOrdersLoaded, Consumer<List<OrderLineView>> onItemsUpdated) {
        Integer uid = Session.getUserId();
        if (uid == null) {
            logger.warning("Nessun utente loggato");
            onOrdersLoaded.accept(List.of());
            return;
        }

        Thread t = new Thread(() -> {
            try {
                List<Order> fullOrders = OrderDAO.listOrdersModel(uid);
                ProcessResult result = processOrders(fullOrders);

                // Salva la cache per uso successivo
                orderCache = result.cache;

                onOrdersLoaded.accept(result.summaries);
                onItemsUpdated.accept(List.of());
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Errore caricamento ordini", e);
                onOrdersLoaded.accept(List.of());
            }
        }, "load-orders-thread");
        t.setDaemon(true);
        t.start();
    }

    public void loadItemsForOrder(int orderId, Consumer<List<OrderLineView>> onItemsLoaded) {
        List<OrderLineView> items = orderCache.getOrDefault(orderId, List.of());
        onItemsLoaded.accept(items);
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

    private record ProcessResult(List<OrderSummaryView> summaries, Map<Integer, List<OrderLineView>> cache) {}
}
