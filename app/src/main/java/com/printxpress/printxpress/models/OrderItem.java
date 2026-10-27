package com.printxpress.printxpress.models;

public class OrderItem {
    private final long id;
    private final String productId;
    private final String productName;
    private final String spec;
    private final int quantity;
    private final double unitPrice;
    private final String filePath;
    private final String remoteFilePath;
    private final String notes;

    public OrderItem(long id, String productId, String productName, String spec, int quantity,
                     double unitPrice, String filePath, String remoteFilePath, String notes) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.spec = spec;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.filePath = filePath;
        this.remoteFilePath = remoteFilePath;
        this.notes = notes;
    }

    public long getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSpec() { return spec; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public String getFilePath() { return filePath; }
    public String getRemoteFilePath() { return remoteFilePath; }
    public String getNotes() { return notes; }
    public double getLineTotal() { return unitPrice * quantity; }
}
