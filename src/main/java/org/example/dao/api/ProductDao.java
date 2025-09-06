package org.example.dao.api;

import org.example.models.Product;
import java.sql.SQLException;
import java.util.List;

public interface ProductDao {
    List<Product> findLatest(int limit) throws SQLException;
    List<Product> searchByName(String name) throws SQLException;
    List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                  double minPrice, double maxPrice) throws SQLException;

    int getShopIdByName(String shopName) throws SQLException;

    List<String> getAvailableSizes(long productId, int idShop) throws SQLException;
    double getPriceFor(long productId, int idShop, String size) throws SQLException;
    Integer getStockFor(long productId, int idShop, String size) throws SQLException;

    boolean existsWish(String username, long productId, int shopId, String size) throws SQLException;
    default boolean existsWish(String username, long productId, int shopId) throws SQLException {
        return existsWish(username, productId, shopId, null);
    }
}
