package org.example.models.entity;

public class Shop {
    private final long idShop;
    private String name;
    private String address;
    private String phone;

    public Shop(long idShop, String name, String address, String phone) {
        if (idShop <= 0) {
            throw new IllegalArgumentException("Shop id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Shop name cannot be blank");
        }

        this.idShop = idShop;
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public long getIdShop() { return idShop; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Shop name cannot be blank");
        }
        this.name = name;
    }

    public void updateAddress(String address) {
        this.address = address;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }
}