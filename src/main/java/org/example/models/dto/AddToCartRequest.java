package org.example.models.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

public record AddToCartRequest(
        long productId,
        int shopId,
        int quantity,
        BigDecimal unitPrice,
        String productName,
        byte[] imageData,
        String size
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddToCartRequest request = (AddToCartRequest) o;
        return productId == request.productId &&
                shopId == request.shopId &&
                quantity == request.quantity &&
                unitPrice.equals(request.unitPrice) &&
                Objects.equals(productName, request.productName) &&
                Objects.equals(size, request.size) &&
                Arrays.equals(imageData, request.imageData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(productId, shopId, quantity, unitPrice, productName, size);
        result = 31 * result + Arrays.hashCode(imageData);
        return result;
    }

    @Override
    public String toString() {
        return "AddToCartRequest{" +
                "productId=" + productId +
                ", shopId=" + shopId +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", productName='" + productName + '\'' +
                ", imageData=" + (imageData != null ? "bytes[" + imageData.length + "]" : "null") +
                ", size='" + size + '\'' +
                '}';
    }
}