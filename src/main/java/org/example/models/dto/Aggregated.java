package org.example.models.dto;

import org.example.models.entity.CartItem;

import java.math.BigDecimal;

public record Aggregated(CartItem sample, int qty) {

    public Aggregated withQty(int newQty) {
        return new Aggregated(sample, newQty);
    }

    public BigDecimal unitPrice() {
        return sample.unitPrice();
    }

    public BigDecimal subtotal() {
        return unitPrice().multiply(BigDecimal.valueOf(qty));
    }
}