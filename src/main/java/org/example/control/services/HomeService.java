package org.example.control.services;

import org.example.dao.ProductDaos;
import org.example.dao.api.ProductDao;
import org.example.models.FilterCriteria;
import org.example.models.Product;

import java.util.List;

public class HomeService {
    private final ProductDao productDao = ProductDaos.create();

    private static final String ALL = "Tutti";

    public List<Product> searchByName(String query){
        return productDao.searchByName(query);
    }

    public List<Product> findLatest(int limit){
        return productDao.findLatest(limit);
    }

    public List<Product> searchByFilters(FilterCriteria criteria) {
        return productDao.searchByFilters(
                criteria.sport().equals(ALL) ? null : criteria.sport(),
                criteria.brand().equals(ALL) ? null : criteria.brand(),
                criteria.shop().equals(ALL) ? null : criteria.shop(),
                criteria.category().equals(ALL) ? null : criteria.category(),
                criteria.minPrice(), criteria.maxPrice()
        );
    }
}

