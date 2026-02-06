package org.example.models;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutData(List<CartItem> items, BigDecimal total) {}
