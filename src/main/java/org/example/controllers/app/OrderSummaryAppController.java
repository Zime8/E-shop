package org.example.controllers.app;

import org.example.models.CartItem;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderSummaryAppController {
    private static final Logger logger = Logger.getLogger(OrderSummaryAppController.class.getName());

    public record ItemView(String productName, String size, int quantity,
                           BigDecimal unitPrice, Object imageObj, BigDecimal subtotal) {}

    public record DisplayData(List<ItemView> rows, BigDecimal total) {}

    public DisplayData processItemsForDisplay(List<CartItem> items, BigDecimal total) {
        var rows = Collections.synchronizedList(new java.util.ArrayList<ItemView>());
        BigDecimal safeTotal = total != null ? total : BigDecimal.ZERO;
        if (items != null) {
            for (CartItem it : items) {
                try {
                    BigDecimal unit = it.getUnitPrice() != null ?
                            BigDecimal.valueOf(it.getUnitPrice()) : BigDecimal.ZERO;
                    BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(it.getQuantity()));
                    rows.add(new ItemView(
                            it.getProductName(), it.getSize(), it.getQuantity(), unit, it.getProductImage(), subtotal
                    ));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Errore caricamento prodotto: ", e);
                }
            }
        }
        return new DisplayData(List.copyOf(rows), safeTotal);
    }
}
