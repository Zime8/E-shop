package org.example.models.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private final int id;
    private final int userId;
    private final LocalDateTime createdAt;
    private OrderStatus status;
    private final List<OrderLine> lines = new ArrayList<>();

    public Order(int id, int userId, LocalDateTime createdAt, OrderStatus status) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
        this.status = status;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return lines; }

    public void setStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }
        this.status = status;
    }

    public void addLine(OrderLine line) {
        if (line != null) {
            lines.add(line);
        }
    }
}