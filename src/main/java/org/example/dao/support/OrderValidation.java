package org.example.dao.support;

import org.example.models.entity.CartItem;

import java.util.List;

public final class OrderValidation {

    private OrderValidation() {
    }

    public static void validateItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Lista articoli vuota");
        }

        for (CartItem it : items) {
            validateCartItem(it);
        }
    }

    private static void validateCartItem(CartItem it) {
        if (it == null) {
            throw new IllegalArgumentException("CartItem nullo");
        }
        if (it.productId() <= 0) {
            throw new IllegalArgumentException("productId non valido");
        }
        if (it.shopId() <= 0) {
            throw new IllegalArgumentException("shopId non valido");
        }
        if (it.size() == null || it.size().isBlank()) {
            throw new IllegalArgumentException("Taglia non valida");
        }
        if (it.quantity() <= 0) {
            throw new IllegalArgumentException("Quantità non valida");
        }
        if (it.unitPrice() == null || it.unitPrice().signum() <= 0) {
            throw new IllegalArgumentException("Prezzo non valido");
        }
    }
}
