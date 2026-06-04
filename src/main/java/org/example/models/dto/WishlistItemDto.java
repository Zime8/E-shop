package org.example.models.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WishlistItemDto that = (WishlistItemDto) o;
        return productId == that.productId &&
                shopId == that.shopId &&
                Objects.equals(name, that.name) &&
                Objects.equals(size, that.size) &&
                Objects.equals(price, that.price) &&
                Arrays.equals(imageData, that.imageData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(productId, shopId, name, size, price);
        result = 31 * result + Arrays.hashCode(imageData);
        return result;
    }

    @Override
    public String toString() {
        return "WishlistItemDto{" +
                "productId=" + productId +
                ", shopId=" + shopId +
                ", name='" + name + '\'' +
                ", size='" + size + '\'' +
                ", price=" + price +
                ", imageData=" + (imageData != null ? "bytes[" + imageData.length + "]" : "null") +
                '}';
    }
}