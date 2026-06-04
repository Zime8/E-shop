package org.example.dao;

import org.example.models.entity.Product;

import java.util.List;

public interface WishlistRepository {
    void addInWishlist(String username, long productId, int shopId, String size);
    void removeInWishlist(String username, long productId, int shopId, String size);
    void clearWishlist(String username);
    List<Product> getFavorites(String username);
    void renameWishlistOwner(String username, String newUsername);
}
