package org.example.models;

public class Shop {
    private long idShop;
    private String name;
    private String address;
    private String phone;

    public Shop(long idShop, String name, String address, String phone) {
        this.idShop = idShop;
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public long getIdShop() { return idShop; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }

    public void setIdShop(long idShop) { this.idShop = idShop; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
}
