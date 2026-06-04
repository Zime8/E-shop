package org.example.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public record Product(
        long productId,
        int idShop,
        String name,
        String sport,
        String brand,
        String category,
        String nameShop,
        BigDecimal price,
        String size,
        byte[] imageData,
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return productId == product.productId &&
                idShop == product.idShop &&
                Objects.equals(name, product.name) &&
                Objects.equals(sport, product.sport) &&
                Objects.equals(brand, product.brand) &&
                Objects.equals(category, product.category) &&
                Objects.equals(nameShop, product.nameShop) &&
                Objects.equals(price, product.price) &&
                Objects.equals(size, product.size) &&
                Objects.equals(createdAt, product.createdAt) &&
                Arrays.equals(imageData, product.imageData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(productId, idShop, name, sport, brand, category, nameShop, price, size, createdAt);
        result = 31 * result + Arrays.hashCode(imageData);
        return result;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", idShop=" + idShop +
                ", name='" + name + '\'' +
                ", sport='" + sport + '\'' +
                ", brand='" + brand + '\'' +
                ", category='" + category + '\'' +
                ", nameShop='" + nameShop + '\'' +
                ", price=" + price +
                ", size='" + size + '\'' +
                ", imageData=" + (imageData != null ? "bytes[" + imageData.length + "]" : "null") +
                ", createdAt=" + createdAt +
                '}';
    }
}