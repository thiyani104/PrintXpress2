package com.printxpress.printxpress.models;

public class CartItem {
    private final long id;
    private final String productId;
    private final String productName;
    private final String spec;
    private final String category;
    private final double unitPrice;
    private final int quantity;
    private final String filePath;
    private final String notes;

    public CartItem(long id, String productId, String productName, String spec, String category,
                    double unitPrice, int quantity, String filePath, String notes) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.spec = spec;
        this.category = category;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.filePath = filePath;
        this.notes = notes;
    }

    public long getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSpec() { return spec; }
    public String getCategory() { return category; }
    public double getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public String getFilePath() { return filePath; }
    public String getNotes() { return notes; }
    public double getLineTotal() { return unitPrice * quantity; }
}
