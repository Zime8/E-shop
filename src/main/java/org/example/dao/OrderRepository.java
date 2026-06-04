package org.example.dao;

import org.example.models.entity.CartItem;
import org.example.models.entity.Order;

import java.util.List;
import java.util.Map;

public interface OrderRepository {

    record CreationResult(
            List<Integer> orderIds,
            Map<Integer, Integer> shopToOrderId
    ) {
        public CreationResult {
            orderIds = List.copyOf(orderIds);
            shopToOrderId = Map.copyOf(shopToOrderId);
        }
    }

    CreationResult placeOrderWithStockDecrement(int userId, List<CartItem> items, String address);

    List<Order> listOrdersModel(int userId);
}
