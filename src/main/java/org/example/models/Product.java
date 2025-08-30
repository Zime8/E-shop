package org.example.models;

import javafx.scene.image.Image;


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
    private Object createdAt;

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
    }

    public static Product copyOf(Product src) {
        return new Product(src);
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNameShop() {
        return nameShop;
    }

    public void setNameShop(String nameShop) {
        this.nameShop = nameShop;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getSize() {return size;}

    public void setSize(String size) {this.size = size;}

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public int getQuantity() {return quantity;}

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    public int getIdShop() {
        return idShop;
    }

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
