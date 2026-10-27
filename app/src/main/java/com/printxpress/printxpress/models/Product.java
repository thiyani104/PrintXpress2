package com.printxpress.printxpress.models;

public class Product {
    private final String id;
    private final String name;
    private final String specDetails;
    private final double price;
    private final double rating;
    private final int imageResource;
    private final String category;
    private final String subTag;

    public Product(String name, String specDetails, double price, double rating,
                   int imageResource, String category, String subTag) {
        this.id = (category + "-" + name).toLowerCase().replaceAll("[^a-z0-9]+", "-");
        this.name = name;
        this.specDetails = specDetails;
        this.price = price;
        this.rating = rating;
        this.imageResource = imageResource;
        this.category = category;
        this.subTag = subTag;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSpecDetails() { return specDetails; }
    public double getPrice() { return price; }
    public double getRating() { return rating; }
    public int getImageResource() { return imageResource; }
    public String getCategory() { return category; }
    public String getSubTag() { return subTag; }
}
