package org.example.models;

public class CartItem {
    private final long productId;
    private final int shopId;
    private final int quantity;
    private final Double unitPrice;
    private final String productName;
    private final byte[] productImage;

    private String size;

    public CartItem(long productId, int shopId, int quantity, Double unitPrice, String productName, byte[] productImage, String size) {
        this.productId = productId;
        this.shopId = shopId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.productName = productName;
        this.productImage = productImage;
        this.size = size;

    }

    public long getProductId() { return productId; }
    public int getShopId() { return shopId; }
    public int getQuantity() { return quantity; }
    public Double getUnitPrice() { return unitPrice; }
    public String getProductName() { return productName; }
    public byte[] getProductImage() { return productImage; }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}
