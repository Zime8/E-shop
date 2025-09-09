package org.example.gateway;

public record PaymentResult(
        boolean success,
        String message,
        String transactionId,
        boolean requiresAction) {
}
