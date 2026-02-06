package org.example.models;

public record ProductOption(int productId, String name, String brand, String sport, String category) {
    @Override public String toString() {
        return name + " · " + brand + " · " + sport + " (" + category + ")";
    }
}
