package org.example.controllers.app;

import org.example.controllers.ui.ProductCardController;
import org.example.dao.ProductDaos;
import org.example.dao.api.ProductDao;
import org.example.demo.DemoData;
import org.example.models.FilterCriteria;
import org.example.models.Product;
import org.example.util.Session;

import java.util.List;
import java.util.logging.Logger;

public class HomeAppController {
    private final ProductDao productDao = ProductDaos.create();

    private static final Logger logger = Logger.getLogger(HomeAppController.class.getName());

    public List<Product> searchByName(String query){
        return productDao.searchByName(query);
    }

    public List<Product> findLatest(int limit){
        return productDao.findLatest(limit);
    }

    public List<Product> searchByFilters(FilterCriteria criteria) {
        return productDao.searchByFilters(
                criteria.sport.equals("Tutti") ? null : criteria.sport,
                criteria.brand.equals("Tutti") ? null : criteria.brand,
                criteria.shop.equals("Tutti") ? null : criteria.shop,
                criteria.category.equals("Tutti") ? null : criteria.category,
                criteria.minPrice, criteria.maxPrice
        );
    }

    public String getCurrentUserName() { return Session.getUser(); }

    public int getCartCount() {
        List<Product> items = getCartItems();
        return items != null ? items.size() : 0;
    }

    public List<Product> getCartItems() {
        return Session.getCartItems();
    }

    public FilterCriteria createFilterCriteria(String sport, String brand, String shop,
                                               String category, double minPrice, double maxPrice) {
        return new FilterCriteria(sport, brand, shop, category, minPrice, maxPrice);
    }

    public FilterCriteria resetFilters() {
        return FilterCriteria.defaults();  // Business logic reset
    }

    public void createProductCard(ProductCardController uiController, Product product) {
        ProductCardAppController appController = new ProductCardAppController();
        uiController.setController(appController);
        uiController.setProduct(product);
    }

    public void logout() {
        if(Session.isDemo()) DemoData.clearUserDemoReviews(Session.getUser());
        Session.clear();
        logger.info("Logout completato - sessione pulita");
    }
}

