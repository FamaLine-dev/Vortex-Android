package com.vortexstore.models;

public class Product {
    private String id;
    private String name;
    private String imageUrl;
    private String category;
    private String badge;
    private double price;
    private String description;

    public Product() {}

    public Product(String id, String name, String imageUrl, String category, String badge, double price) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.category = category;
        this.badge = badge;
        this.price = price;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
