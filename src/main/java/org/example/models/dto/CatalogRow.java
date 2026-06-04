package org.example.models.dto;

import java.math.BigDecimal;

public record CatalogRow(
        int productId, String name, String sport, String brand,
        String category, String size, BigDecimal price, int quantity) {}
