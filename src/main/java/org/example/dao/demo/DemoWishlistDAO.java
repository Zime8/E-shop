package org.example.dao.demo;

import org.example.dao.WishlistRepository;
import org.example.demo.DemoData;
import org.example.models.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class DemoWishlistDAO implements WishlistRepository {

    @Override
    public void addInWishlist(String username, long productId, int shopId, String size){

        requireNonBlank(username);

        DemoData.ensureLoaded();
        String key = DemoData.prodKey(productId, shopId, size);
        Product p = DemoData.products().get(key);

        System.out.println("WISHLIST add -> key=" + key + ", found=" + (p != null)
                + ", productId=" + productId + ", shopId=" + shopId + ", size=" + size);

        if (p == null) {
            p = new Product(
                    productId,
                    shopId,
                    "Prodotto #" + productId,
                    "N/D",
                    "Demo",
                    "N/D",
                    "Shop #" + shopId,
                    BigDecimal.ZERO,
                    size,
                    null,
                    LocalDateTime.now()
            );
        }

        DemoData.wishlists().computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
        DemoData.wishlists().get(username).removeIf(ex ->
                ex.productId() == productId &&
                        ex.idShop() == shopId &&
                        Objects.equals(ex.size(), size));
        DemoData.wishlists().get(username).add(p);
    }


    @Override
    public void removeInWishlist(String username, long productId, int shopId, String size){

        requireNonBlank(username);

        DemoData.ensureLoaded();
        List<Product> list = DemoData.wishlists().getOrDefault(username, Collections.emptyList());
        list.removeIf(p -> p.productId() == productId &&
                p.idShop() == shopId &&
                Objects.equals(p.size(), size));
    }

    @Override
    public void clearWishlist(String username){

        requireNonBlank(username);

        DemoData.ensureLoaded();
        DemoData.wishlists().remove(username);
    }

    @Override
    public List<Product> getFavorites(String username){

        requireNonBlank(username);

        DemoData.ensureLoaded();
        return new ArrayList<>(DemoData.wishlists().getOrDefault(username, Collections.emptyList()));
    }

    @Override
    public void renameWishlistOwner(String currentUsername, String newUsername) {
        requireNonBlank(currentUsername);
        requireNonBlank(newUsername);

        if (Objects.equals(currentUsername, newUsername)) {
            return;
        }

        DemoData.ensureLoaded();
        var wl = DemoData.wishlists().remove(currentUsername);
        if (wl != null) {
            DemoData.wishlists().put(newUsername, wl);
        }
    }

    private void requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username non valido");
        }
    }
}
