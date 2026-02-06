package org.example.controllers.app;

import org.example.controllers.ui.ProductCardController;
import org.example.models.FilterCriteria;
import org.example.models.Product;
import org.example.control.services.HomeService;

import java.util.List;

public class HomeAppController {
    private final HomeService service = new HomeService();

    public List<Product> searchByName(String query) { return service.searchByName(query); }
    public List<Product> findLatest(int limit) { return service.findLatest(limit); }
    public List<Product> searchByFilters(FilterCriteria criteria) { return service.searchByFilters(criteria); }
    public String getCurrentUserName() { return service.getCurrentUserName(); }
    public int getCartCount() {
        List<Product> items = service.getCartItems();
        return items != null ? items.size() : 0;
    }
    public FilterCriteria createFilterCriteria(String sport, String brand, String shop, String category, double minPrice, double maxPrice) {
        return new FilterCriteria(sport, brand, shop, category, minPrice, maxPrice);
    }
    public FilterCriteria resetFilters() {
        return FilterCriteria.defaults();
    }
    public void createProductCard(ProductCardController uiController, Product product) {
        ProductCardAppController appController = new ProductCardAppController();
        uiController.setController(appController);
        uiController.setProduct(product);
    }
    public void logout() { service.logout(); }
}

