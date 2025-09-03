package org.example.models;

public class User {
    private String username;
    private String email;
    private String phone;
    private String passwordHash;

    public String getUsername() {return username;}
    public String getEmail() {return email;}
    public String getPhone() {return phone;}
    public String getPasswordHash() {return passwordHash;}

    public void setUsername(String username) {this.username = username;}
    public void setEmail(String email) {this.email = email;}
    public void setPhone(String phone) {this.phone = phone;}
    public void setPasswordHash(String passwordHash) {this.passwordHash = passwordHash;}

}
