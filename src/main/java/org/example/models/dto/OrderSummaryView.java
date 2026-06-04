package org.example.models.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record OrderSummaryView(int idOrder, Timestamp dateOrder, String stateOrder, BigDecimal totalAmount) {}
