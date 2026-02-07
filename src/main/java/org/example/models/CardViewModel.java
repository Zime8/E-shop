package org.example.models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class CardViewModel {
    private final SimpleIntegerProperty id = new SimpleIntegerProperty();
    private final SimpleStringProperty holder = new SimpleStringProperty();
    private final SimpleStringProperty number = new SimpleStringProperty();
    private final SimpleStringProperty expiry = new SimpleStringProperty();
    private final SimpleStringProperty type = new SimpleStringProperty();

    // Costruttore
    public CardViewModel(Card entity) {
        if (entity != null) {
            id.set(entity.id());
            holder.set(entity.holder());
            number.set(entity.number());
            expiry.set(entity.expiry());
            type.set(entity.type());
        }
    }

    // Getters
    public int getId() { return id.get(); }
    public String getHolder() { return holder.get(); }
    public String getNumber() { return number.get(); }
    public String getExpiry() { return expiry.get(); }
    public String getType() { return type.get(); }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setHolder(String value) { holder.set(value); }
    public void setNumber(String value) { number.set(value); }
    public void setExpiry(String value) { expiry.set(value); }
    public void setType(String value) { type.set(value); }

    // PROPERTY methods per TableView
    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleStringProperty holderProperty() { return holder; }
    public SimpleStringProperty expiryProperty() { return expiry; }
    public SimpleStringProperty typeProperty() { return type; }

    public Card toEntity() {
        return new Card(getId(), getHolder(), getNumber(), getExpiry(), getType());
    }
}
