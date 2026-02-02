package org.example.models;

import java.math.BigDecimal;

public record CatalogForm(int productId, String size, BigDecimal price, int quantity) {}

