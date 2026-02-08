package org.example.models;

import java.math.BigDecimal;
import java.util.List;

public record PaymentData(List<CartItem> items, BigDecimal total, Runnable onCartUpdated) {}

