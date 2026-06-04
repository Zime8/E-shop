package org.example.models.dto;

import java.math.BigDecimal;

public record CatalogForm(int productId, String size, BigDecimal price, int quantity) {}

