package org.example.controllers.app;

import org.example.dao.OrderDAO;
import org.example.dao.OrderDAO.OrderLine;
import org.example.models.Order;
import org.example.util.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PurchaseHistoryAppController {

    private static final Logger logger = Logger.getLogger(PurchaseHistoryAppController.class.getName());

    // Cache per i dettagli degli ordini
    private Map<Integer, List<OrderLineView>> orderCache = new java.util.HashMap<>();

    public void loadOrders(Consumer<List<OrderSummaryView>> onOrdersLoaded, Consumer<List<OrderLineView>> onItemsUpdated) {
        Integer uid = Session.getUserId();
        if (uid == null) {
            logger.warning("Nessun utente loggato");
            onOrdersLoaded.accept(List.of());  // Lista vuota
            return;
        }

        Thread t = new Thread(() -> {
            try {
                List<Order> fullOrders = OrderDAO.listOrdersModel(uid);
                ProcessResult result = processOrders(fullOrders);

                // Salva la cache per uso successivo
                orderCache = result.cache;

                onOrdersLoaded.accept(result.summaries);
                onItemsUpdated.accept(List.of());  // Iniziale vuoto
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
            List<OrderDAO.OrderLine> daoLines = o.getLines().stream()
                    .map(this::modelOrderLineToDaoOrderLine)
                    .toList();

            List<OrderLineView> viewLines = daoLines.stream()
                    .map(this::modelLineToView)
                    .toList();

            newCache.put(o.getId(), viewLines);

            BigDecimal total = daoLines.stream()
                    .map(OrderLine::getSubtotal)
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

    private OrderDAO.OrderLine modelOrderLineToDaoOrderLine(org.example.models.OrderLine modelLine) {
        return new OrderDAO.OrderLine(
                modelLine.getOrderId(), modelLine.getProductId(), modelLine.getShopId(),
                modelLine.getProductName(), modelLine.getShopName(), modelLine.getSize(),
                modelLine.getQuantity(), modelLine.getUnitPrice()
        );
    }

    private OrderLineView modelLineToView(OrderLine l) {
        return new OrderLineView(
                l.orderId(),
                l.productId(),
                l.shopId(),
                l.productName(),
                l.shopName(),
                l.size(),
                l.quantity(),
                l.unitPrice(),
                l.getSubtotal()
        );
    }

    private record ProcessResult(List<OrderSummaryView> summaries, Map<Integer, List<OrderLineView>> cache) {}

    public record OrderSummaryView(int idOrder, Timestamp dateOrder, String stateOrder, BigDecimal totalAmount) {}
    public record OrderLineView(int orderId, long productId, int shopId, String productName, String shopName,
                                String size, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}
