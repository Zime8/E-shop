package org.example.dao.demo;

import org.example.dao.ProductRepository;
import org.example.demo.DemoData;
import org.example.models.entity.Product;

import java.util.*;

public class DemoProductDAO implements ProductRepository {

    @Override
    public List<Product> findLatest(int limit) {
        DemoData.ensureLoaded();
        return DemoData.products().values().stream()
                .sorted(Comparator.comparing(Product::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<Product> searchByName(String name) {
        DemoData.ensureLoaded();
        String q = normalize(name);
        return DemoData.products().values().stream()
                .filter(p -> normalize(p.name()).contains(q))
                .sorted(Comparator.comparing(Product::createdAt).reversed())
                .toList();
    }

    @Override
    public List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                         double minPrice, double maxPrice) {
        DemoData.ensureLoaded();

        return DemoData.products().values().stream()
                .filter(p -> isBlank(sport) || equalsIgnoreCase(p.sport(), sport))
                .filter(p -> isBlank(brand) || equalsIgnoreCase(p.brand(), brand))
                .filter(p -> isBlank(shop) || equalsIgnoreCase(p.nameShop(), shop))
                .filter(p -> isBlank(category) || equalsIgnoreCase(p.category(), category))
                .filter(p -> p.price() != null && p.price().doubleValue() >= minPrice && p.price().doubleValue() <= maxPrice)
                .sorted(Comparator.comparing(Product::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<Product> findById(long productId) {
        DemoData.ensureLoaded();
        return DemoData.products().values().stream()
                .filter(p -> p.productId() == productId)
                .findFirst();
    }

    @Override
    public int getShopIdByName(String shopName) {
        DemoData.ensureLoaded();
        return DemoData.products().values().stream()
                .filter(p -> equalsIgnoreCase(p.nameShop(), shopName))
                .map(Product::idShop)
                .findFirst()
                .orElse(0);
    }

    @Override
    public List<String> getAvailableSizes(long productId, int idShop) {
        DemoData.ensureLoaded();
        return DemoData.products().values().stream()
                .filter(p -> p.productId() == productId && p.idShop() == idShop)
                .map(Product::size)
                .distinct()
                .toList();
    }

    @Override
    public double getPriceFor(long productId, int idShop, String size) {
        DemoData.ensureLoaded();
        return DemoData.products().values().stream()
                .filter(p -> p.productId() == productId && p.idShop() == idShop && sameSize(p.size(), size))
                .map(Product::price)
                .filter(Objects::nonNull)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .findFirst()
                .orElse(0.0);
    }

    @Override
    public Integer getStockFor(long productId, int idShop, String size) {
        DemoData.ensureLoaded();
        return DemoData.stock().getOrDefault(DemoData.stockKey(productId, idShop, size), 0);
    }

    @Override
    public boolean existsWish(String username, long productId, int shopId, String size) {
        DemoData.ensureLoaded();
        return DemoData.wishlists().getOrDefault(username, List.of()).stream()
                .anyMatch(p -> p.productId() == productId
                        && p.idShop() == shopId
                        && sameSize(p.size(), size));
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private boolean sameSize(String a, String b) {
        return normalize(a).equals(normalize(b));
    }
}