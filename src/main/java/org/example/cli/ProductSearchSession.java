package org.example.cli;

import org.example.models.entity.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ProductSearchSession {

    private final List<Product> lastSearch = new ArrayList<>();

    void replaceAll(List<Product> products) {
        lastSearch.clear();
        lastSearch.addAll(products);
    }

    Optional<Product> find(long productId, int shopId) {
        return lastSearch.stream()
                .filter(p -> p.productId() == productId && p.idShop() == shopId)
                .findFirst();
    }
}