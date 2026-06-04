package org.example.models.dto;

public record CheckoutRequest(
        Card card,
        String cvv,
        String address
) {}
