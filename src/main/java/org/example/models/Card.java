package org.example.models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Card {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty holder;
    private final SimpleStringProperty number;
    private final SimpleStringProperty expiry;
    private final SimpleStringProperty type;

    public Card(int id, String holder, String number, String expiry, String type) {
        this.id = new SimpleIntegerProperty(id);
        this.holder = new SimpleStringProperty(holder);
        this.number = new SimpleStringProperty(number);
        this.expiry = new SimpleStringProperty(expiry);
        this.type = new SimpleStringProperty(type);
    }

    public Card(String holder, String number, String expiry, String type) {
        this(0, holder, number, expiry, type);
    }

    public int getId() {return id.get();}
    public String getHolder() { return holder.get(); }
    public String getNumber() { return number.get(); }
    public String getExpiry() { return expiry.get(); }
    public String getType() { return type.get(); }

    public void setId(int id) { this.id.set(id); }
    public void setHolder(String value) { holder.set(value); }
    public void setNumber(String value) { number.set(value); }
    public void setExpiry(String value) { expiry.set(value); }
    public void setType(String value) { type.set(value); }

    // METODI PROPERTY per TableView
    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleStringProperty holderProperty() { return holder; }
    public SimpleStringProperty numberProperty() { return number; }
    public SimpleStringProperty expiryProperty() { return expiry; }
    public SimpleStringProperty typeProperty() { return type; }
}
