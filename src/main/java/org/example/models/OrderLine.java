package org.example.models;

import java.math.BigDecimal;

public class OrderLine {
    private int orderId;
    private long productId;
    private int shopId;
    private String productName;
    private String shopName;
    private String size;
    private int quantity;
    private BigDecimal unitPrice;

    // Parameter Object per i dettagli
    public record Details(String productName, String shopName, String size, int quantity, BigDecimal unitPrice) {}

    // Costruttore con 4 parametri (3 id + oggetto dettagli)
    public OrderLine(int orderId, long productId, int shopId, Details details) {
        this.orderId = orderId;
        this.productId = productId;
        this.shopId = shopId;
        if (details != null) {
            this.productName = details.productName();
            this.shopName = details.shopName();
            this.size = details.size();
            this.quantity = details.quantity();
            this.unitPrice = details.unitPrice();
        }
    }

    public int getOrderId() { return orderId; }
    public long getProductId() { return productId; }
    public int getShopId() { return shopId; }
    public String getProductName() { return productName; }
    public String getShopName() { return shopName; }
    public String getSize() { return size; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setProductId(long productId) { this.productId = productId; }
    public void setShopId(int shopId) { this.shopId = shopId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public void setSize(String size) { this.size = size; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
