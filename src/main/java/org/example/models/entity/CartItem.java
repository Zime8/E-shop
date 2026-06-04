package org.example.models.entity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

public record CartItem(
        long productId,
        int shopId,
        int quantity,
        BigDecimal unitPrice,
        String productName,
        byte[] productImage,
        String size
) {
    public CartItem {
        if (productId <= 0) {
            throw new IllegalArgumentException("Invalid productId");
        }
        if (shopId <= 0) {
            throw new IllegalArgumentException("Invalid shopId");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid unit price");
        }
        productImage = productImage != null ? Arrays.copyOf(productImage, productImage.length) : null;
    }

    @Override
    public byte[] productImage() {
        return productImage != null ? Arrays.copyOf(productImage, productImage.length) : null;
    }

    public CartItem withQuantity(int newQuantity) {
        return new CartItem(productId, shopId, newQuantity, unitPrice, productName, productImage == null ? null : productImage(), size);
    }

    public CartItem withSize(String newSize) {
        return new CartItem(productId, shopId, quantity, unitPrice, productName, productImage == null ? null : productImage(), newSize);
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return productId == cartItem.productId &&
                shopId == cartItem.shopId &&
                quantity == cartItem.quantity &&
                unitPrice.equals(cartItem.unitPrice) &&
                Objects.equals(productName, cartItem.productName) &&
                Objects.equals(size, cartItem.size) &&
                Arrays.equals(productImage, cartItem.productImage);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(productId, shopId, quantity, unitPrice, productName, size);
        result = 31 * result + Arrays.hashCode(productImage);
        return result;
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "productId=" + productId +
                ", shopId=" + shopId +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", productName='" + productName + '\'' +
                ", productImage=" + (productImage != null ? "bytes[" + productImage.length + "]" : "null") +
                ", size='" + size + '\'' +
                '}';
    }
}