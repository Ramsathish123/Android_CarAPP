package com.example.carspa;

public class Product {
    public String itemName;
    public int quantity;
    public double rate;

    public Product() {
        // Default constructor required for Firebase
    }

    public Product(String itemName, int quantity, double rate) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.rate = rate;
    }
}
