package org.example.control.services;

import org.example.dao.ShopRepository;
import org.example.dao.ProductRepository;
import org.example.models.dto.ShopDto;
import org.example.models.entity.Shop;

import java.util.List;

public class ProductDetailService {

    private final ProductRepository productDao;
    private final ShopRepository shopRepository;

    public ProductDetailService(ProductRepository productDao, ShopRepository shopRepository) {
        this.productDao = productDao;
        this.shopRepository = shopRepository;
    }

    public ShopDto getShopInfo(int shopId) {
        Shop shop = shopRepository.getById(shopId);
        if (shop == null) return null;

        return new ShopDto(
                shop.getIdShop(),
                shop.getName(),
                shop.getAddress(),
                shop.getPhone()
        );
    }

    public List<String> getAvailableSizes(long productId, int shopId) {
        return productDao.getAvailableSizes(productId, shopId);
    }

    public double getPriceFor(long productId, int shopId, String size) {
        return productDao.getPriceFor(productId, shopId, size);
    }

    public Integer getStockFor(long productId, int shopId, String size) {
        return productDao.getStockFor(productId, shopId, size);
    }

    public boolean isValidQuantity(int quantity, int maxStock) {
        return quantity > 0 && maxStock >= 0 && quantity <= maxStock;
    }
}