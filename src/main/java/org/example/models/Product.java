package org.example.models;

import javafx.scene.image.Image;

import java.time.LocalDateTime;

public class Product {

    private long productId;
    private String name;
    private String sport;
    private String brand;
    private String category;
    private int quantity;

    private String nameShop;

    private double price;
    private String size;
    private Image image;
    private LocalDateTime createdAt;
    private byte[] imageData;

    private int idShop;

    public Product() {
        // Costruttore vuoto
    }

    public Product(Product src) {
        if (src == null) return;
        this.productId = src.getProductId();
        this.idShop = src.getIdShop();
        this.name = src.getName();
        this.brand = src.getBrand();
        this.category = src.getCategory();
        this.quantity = src.getQuantity();
        this.sport = src.getSport();
        this.nameShop = src.getNameShop();
        this.price = src.getPrice();
        this.size = src.getSize();
        this.image = src.getImage();
        this.createdAt = src.getCreatedAt();
        this.imageData = src.getImageData();
    }

    public static Product fromCartItem(CartItem item) {
        Product p = new Product();
        p.setProductId(item.getProductId());
        p.setIdShop(item.getShopId());
        p.setName(item.getProductName());
        p.setPrice(item.getUnitPrice());
        p.setImageData(item.getProductImage());
        p.setSize(item.getSize());
        return p;
    }

    public long getProductId() { return productId;}
    public String getName() {
        return name;
    }
    public String getSport() {
        return sport;
    }
    public String getBrand() {
        return brand;
    }
    public String getCategory() {
        return category;
    }
    public String getNameShop() {
        return nameShop;
    }
    public double getPrice() {
        return price;
    }
    public String getSize() {return size;}
    public Image getImage() {
        return image;
    }
    public int getQuantity() {return quantity;}
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public byte[] getImageData() {return imageData;}
    public int getIdShop() {
        return idShop;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setSport(String sport) {
        this.sport = sport;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public void setNameShop(String nameShop) {
        this.nameShop = nameShop;
    }
    public void setPrice(double price) {
        this.price = price;
    }
    public void setSize(String size) {this.size = size;}
    public void setImage(Image image) {
        this.image = image;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public void setImageData(byte[] imageData) {this.imageData = imageData;}
    public void setIdShop(int idShop) {
        this.idShop = idShop;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", name='" + name + '\'' +
                ", sport='" + sport + '\'' +
                ", brand='" + brand + '\'' +
                ", category='" + category + '\'' +
                ", nameShop='" + nameShop + '\'' +
                ", price=" + price +
                ", image=" + image +
                ", createdAt=" + createdAt +
                ", id_shop=" + idShop +
                '}';
    }
}
