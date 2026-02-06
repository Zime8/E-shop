package org.example.models;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record OrderSummaryView(int idOrder, Timestamp dateOrder, String stateOrder, BigDecimal totalAmount) {}
