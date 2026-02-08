package org.example.controllers.app;

import org.example.dao.ProductDaos;
import org.example.dao.ShopDAO;
import org.example.dao.UserDAO;
import org.example.dao.api.ProductDao;
import org.example.models.CartItem;
import org.example.models.Shop;
import org.example.util.Session;

import java.util.List;

public class ProductDetailAppController {

    private final ProductDao productDao;
    private final UserDAO userDao;

    public ProductDetailAppController(ProductDao productDao) {
        this.productDao = productDao;
        this.userDao = new UserDAO();
    }

    @SuppressWarnings("unused") // Navigator
    public ProductDetailAppController() {
        this(ProductDaos.create());
    }

    public Shop getShopInfo(int shopId) {
        return ShopDAO.getById(shopId);
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

    public boolean existsWish(String user, long productId, int shopId) {
        return productDao.existsWish(user, productId, shopId);
    }

    public boolean existsWish(String user, long productId, int shopId, String size) {
        return productDao.existsWish(user, productId, shopId, size);
    }

    public void addToWishList(String user, long productId, int shopId, String size) {
        userDao.addInWishList(user, productId, shopId, size);
    }

    public void addToCart(CartItem item, int quantity) {
        CartItem qtyItem = item.withQuantity(quantity);
        Session.addToCart(qtyItem);
    }

    public void addToCart(CartItem item) {
        addToCart(item, item.getQuantity());
    }

    public boolean isValidQuantity(int quantity, int maxStock) {
        return quantity > 0 && quantity <= maxStock;
    }
}

