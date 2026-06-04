package org.example.models.dto;

import java.math.BigDecimal;

public record OrderLineView(int orderId, long productId, int shopId, String productName, String shopName,
                            String size, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
