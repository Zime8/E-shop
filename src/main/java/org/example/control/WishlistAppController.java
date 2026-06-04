package org.example.control;

import org.example.control.services.CartService;
import org.example.control.session.UserContext;
import org.example.dao.WishlistRepository;
import org.example.dao.ProductRepository;
import org.example.models.dto.WishlistItemDto;
import org.example.models.entity.Product;

import java.util.List;

public class WishlistAppController {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productDao;
    private final UserContext userContext;
    private final CartService cartService;

    public WishlistAppController(WishlistRepository wishlistRepository, ProductRepository productDao,
                                 UserContext userContext, CartService cartService) {
        this.wishlistRepository = wishlistRepository;
        this.productDao = productDao;
        this.userContext = userContext;
        this.cartService = cartService;
    }

    public List<WishlistItemDto> loadItems() {
        List<Product> products = wishlistRepository.getFavorites(userContext.getCurrentUsername());
        return products.stream()
                .map(this::toDto)
                .toList();
    }

    public boolean existsWish(long productId, int shopId) {
        return productDao.existsWish(userContext.getCurrentUsername(), productId, shopId);
    }
    public boolean existsWish(long productId, int shopId, String size) {
        return productDao.existsWish(userContext.getCurrentUsername(), productId, shopId, size);
    }
    public void addToWishlist(long productId, int shopId, String size) {
        wishlistRepository.addInWishlist(userContext.getCurrentUsername(), productId, shopId, size);
    }

    public void removeFromWishlist(WishlistItemDto p) {
        wishlistRepository.removeInWishlist(
                userContext.getCurrentUsername(),
                p.productId(),
                p.shopId(),
                p.size()
        );
    }

    public void addToCart(WishlistItemDto p) {
        cartService.addWishlistItemToCart(p);
    }

    public void clearWishlist() {
        wishlistRepository.clearWishlist(userContext.getCurrentUsername());
    }

    private WishlistItemDto toDto(Product p) {
        return new WishlistItemDto(
                p.productId(),
                p.idShop(),
                p.name(),
                p.size(),
                p.price(),
                p.imageData()
        );
    }
}
