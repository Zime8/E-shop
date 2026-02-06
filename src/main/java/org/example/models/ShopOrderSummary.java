package org.example.models;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record ShopOrderSummary(
        int orderId, Timestamp orderDate, String state,
        BigDecimal total, String customer, String address) {}
