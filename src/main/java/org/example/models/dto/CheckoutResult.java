package org.example.models.dto;

public record CheckoutResult(
        boolean success,
        String message,
        String transactionId,
        String orderIds
) {}
