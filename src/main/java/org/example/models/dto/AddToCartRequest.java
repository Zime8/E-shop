package org.example.models.dto;

import java.math.BigDecimal;

public record AddToCartRequest(
        long productId,
        int shopId,
        int quantity,
        BigDecimal unitPrice,
        String productName,
        byte[] imageData,
        String size
) {}
