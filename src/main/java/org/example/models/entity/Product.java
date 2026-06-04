package org.example.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Product(
        long productId, int idShop, String name, String sport, String brand,
        String category, String nameShop, BigDecimal price, String size, byte[] imageData,
        LocalDateTime createdAt
) {
    public Product {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid price");
        }
    }
}
