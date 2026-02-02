package org.example.models;

public class FilterCriteria {
    public final String sport, brand, shop, category;
    public final double minPrice, maxPrice;

    public FilterCriteria(String sport, String brand, String shop, String category,
                          double minPrice, double maxPrice) {
        this.sport = sport;
        this.brand = brand;
        this.shop = shop;
        this.category = category;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public static FilterCriteria defaults() {
        return new FilterCriteria("Tutti", "Tutti", "Tutti", "Tutti", 0.0, Double.MAX_VALUE);
    }
}

