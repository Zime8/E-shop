package org.example.models.dto;

import java.math.BigDecimal;
import java.util.List;

public record DisplayData(List<ItemView> rows, BigDecimal total) { }
