package org.example.control.services;

import org.example.dao.ProductDaos;
import org.example.dao.api.ProductDao;
import org.example.demo.DemoData;
import org.example.models.FilterCriteria;
import org.example.models.Product;
import org.example.util.Session;

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

    public String getCurrentUserName() { return Session.getUser(); }

    public List<Product> getCartItems() {
        return Session.getCartItems();
    }

    public void logout() {
        if(Session.isDemo()) DemoData.clearUserDemoReviews(Session.getUser());
        Session.logout();
    }
}

