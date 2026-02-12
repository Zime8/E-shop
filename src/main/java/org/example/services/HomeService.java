package org.example.services;

import org.example.dao.ProductDaos;
import org.example.dao.api.ProductDao;
import org.example.models.FilterCriteria;
import org.example.models.Product;

import java.util.List;

public class HomeService {
    private final ProductDao productDao = ProductDaos.create();

    private static final String ALL = "Tutti";

    public List<Product> searchByName(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return productDao.searchByName(query.trim());
    }

    public List<Product> findLatest(int limit) {
        if (limit <= 0) {
            limit = 40;
        }
        return productDao.findLatest(Math.min(limit, 100));
    }

    public List<Product> searchByFilters(FilterCriteria criteria) {
        if (criteria == null) {
            return List.of();
        }
        return productDao.searchByFilters(
                allToNull(criteria.sport()),
                allToNull(criteria.brand()),
                allToNull(criteria.shop()),
                allToNull(criteria.category()),
                criteria.minPrice(),
                criteria.maxPrice()
        );
    }

    private String allToNull(String value) {
        return ALL.equals(value) ? null : value;
    }
}

