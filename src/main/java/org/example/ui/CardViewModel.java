package org.example.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import org.example.models.dto.Card;

public class CardViewModel {
    private final SimpleIntegerProperty id = new SimpleIntegerProperty();
    private final SimpleStringProperty holder = new SimpleStringProperty();
    private final SimpleStringProperty number = new SimpleStringProperty();
    private final SimpleStringProperty expiry = new SimpleStringProperty();
    private final SimpleStringProperty type = new SimpleStringProperty();

    // Costruttore
    public CardViewModel(Card card) {
        if (card != null) {
            id.set(card.id());
            holder.set(card.holder());
            number.set(card.number());
            expiry.set(card.expiry());
            type.set(card.type());
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

    public Card toDto() {
        return new Card(getId(), getHolder(), getNumber(), getExpiry(), getType());
    }
}
