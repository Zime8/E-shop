package org.example.models;

import java.math.BigDecimal;

public record ShopOrderLine(
        long productId, String productName, String size,
        int quantity, BigDecimal unitPrice) {
    public BigDecimal subtotal() {
        return unitPrice == null ? BigDecimal.ZERO :
                unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
