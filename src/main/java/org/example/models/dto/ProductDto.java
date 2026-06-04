package org.example.models.dto;

import java.util.Arrays;
import java.util.Objects;

public record ProductDto(
        long productId,
        int shopId,
        String name,
        String nameShop,
        String sport,
        double unitPrice,
        byte[] imageData,
        int stock,
        String size
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDto productDto = (ProductDto) o;
        return productId == productDto.productId &&
                shopId == productDto.shopId &&
                Double.compare(productDto.unitPrice, unitPrice) == 0 &&
                stock == productDto.stock &&
                Objects.equals(name, productDto.name) &&
                Objects.equals(nameShop, productDto.nameShop) &&
                Objects.equals(sport, productDto.sport) &&
                Objects.equals(size, productDto.size) &&
                Arrays.equals(imageData, productDto.imageData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(productId, shopId, name, nameShop, sport, unitPrice, stock, size);
        result = 31 * result + Arrays.hashCode(imageData);
        return result;
    }

    @Override
    public String toString() {
        return "ProductDto{" +
                "productId=" + productId +
                ", shopId=" + shopId +
                ", name='" + name + '\'' +
                ", nameShop='" + nameShop + '\'' +
                ", sport='" + sport + '\'' +
                ", unitPrice=" + unitPrice +
                ", imageData=" + (imageData != null ? "bytes[" + imageData.length + "]" : "null") +
                ", stock=" + stock +
                ", size='" + size + '\'' +
                '}';
    }
}