package org.example.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private int userId;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private final List<OrderLine> lines = new ArrayList<>();

    public Order() {}

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

    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public void addLine(OrderLine l) { lines.add(l); }

    public BigDecimal getTotal() {
        return lines.stream()
                .map(OrderLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
