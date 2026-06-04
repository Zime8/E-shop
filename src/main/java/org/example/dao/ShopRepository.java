package org.example.dao;

import org.example.models.dto.SellerShop;
import org.example.models.entity.Shop;

import java.math.BigDecimal;
import java.util.Optional;

public interface ShopRepository {
    BigDecimal getBalance(long userId);
    void requestWithdraw(long userId, BigDecimal amount);
    Shop getById(long idShop);
    Optional<SellerShop> findShopForUser(long userId);
}