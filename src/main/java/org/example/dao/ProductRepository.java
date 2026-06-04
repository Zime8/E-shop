package org.example.dao;

import org.example.models.entity.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> findLatest(int limit);
    List<Product> searchByName(String name) ;
    List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                  double minPrice, double maxPrice);
    Optional<Product> findById(long productId);

    int getShopIdByName(String shopName);

    List<String> getAvailableSizes(long productId, int idShop);
    double getPriceFor(long productId, int idShop, String size);
    Integer getStockFor(long productId, int idShop, String size);

    boolean existsWish(String username, long productId, int shopId, String size);
    default boolean existsWish(String username, long productId, int shopId) {
        return existsWish(username, productId, shopId, null);
    }
}
