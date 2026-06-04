package org.example.dao.gateway;

public record PaymentResult(
        boolean success,
        String message,
        String transactionId,
        boolean requiresAction) {
}
