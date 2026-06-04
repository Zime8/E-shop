package org.example.models.dto;

import org.example.models.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutData(List<CartItem> items, BigDecimal total) {}
