package org.example.gateway;

/**
 * @param transactionId  opzionale
 * @param requiresAction es. 3DS
 */
public record PaymentResult(
        boolean success,
        String message,
        String transactionId,
        boolean requiresAction) {
}
