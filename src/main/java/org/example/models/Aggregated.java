package org.example.models;

public class Aggregated {
    public final Product sample;
    private int qty;
    public Aggregated(Product sample, int qty) { this.sample = sample; this.qty = qty; }
    public int getQty() { return qty; }

    public void incrementQty() {
        qty++;
    }

    public double unitPrice() { return sample.getPrice(); }
    public double subtotal() { return unitPrice() * qty; }
}
