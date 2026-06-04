package org.example.models.dto;

public record ProductDto(
        long productId, int shopId, String name, String nameShop, String sport,
        double unitPrice, byte[] imageData, int stock, String size
) {
    public String displayPrice() { return String.format("€%.2f", unitPrice); }
}


