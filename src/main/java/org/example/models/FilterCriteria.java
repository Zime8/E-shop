package org.example.models;

public record FilterCriteria(
        String sport,
        String brand,
        String shop,
        String category,
        double minPrice,
        double maxPrice
) {
    public static FilterCriteria defaults() {
        return new FilterCriteria("Tutti", "Tutti", "Tutti", "Tutti", 0.0, Double.MAX_VALUE);
    }
}


