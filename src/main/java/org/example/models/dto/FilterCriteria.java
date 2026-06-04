package org.example.models.dto;

public record FilterCriteria(
        String sport,
        String brand,
        String shop,
        String category,
        double minPrice,
        double maxPrice
) {
    private static final String ALL = "Tutti";

    public static FilterCriteria defaults() {
        return new FilterCriteria(ALL, ALL, ALL, ALL, 0.0, Double.MAX_VALUE);
    }
}


