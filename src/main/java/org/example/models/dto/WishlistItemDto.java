package org.example.models.dto;

import java.math.BigDecimal;

public record WishlistItemDto(
        long productId,
        int shopId,
        String name,
        String size,
        BigDecimal price,
        byte[] imageData
) {
    public String displayPrice() {
        return String.format("€%.2f", price);
    }
}
