package com.printxpress.printxpress.models;

import com.printxpress.printxpress.database.DBContract.OrderEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Order {

    public static final String DB_DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private long id;
    private String createdAt;
    private String status;
    private double subtotal;
    private double discount;
    private String promoCode;
    private double deliveryFee;
    private double total;
    private String deliveryMethod;
    private String address;
    private String deliveryDate;
    private String paymentMethod;
    private String cancelReason;
    private String remoteId;
    private boolean synced;

    // Filled for list rows
    private String firstItemName = "";
    private int itemCount;
    private int totalQuantity;

    // Filled for the details screen
    private List<OrderItem> items = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
    public String getFirstItemName() { return firstItemName; }
    public void setFirstItemName(String firstItemName) { this.firstItemName = firstItemName; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public static String displayId(long id) {
        return "#PX" + (10000 + id);
    }

    public String getDisplayId() {
        return displayId(id);
    }

    public boolean isPending() {
        return OrderEntry.STATUS_PENDING.equals(status);
    }

    public boolean isCancelled() {
        return OrderEntry.STATUS_CANCELLED.equals(status);
    }

    public boolean isActive() {
        return !isCancelled() && !OrderEntry.STATUS_COMPLETED.equals(status);
    }

    /** 0 = Pending ... 3 = Completed, -1 when cancelled. */
    public int getProgressStep() {
        if (isCancelled()) return -1;
        if (OrderEntry.STATUS_PROCESSING.equals(status)) return 1;
        if (OrderEntry.STATUS_PRINTING.equals(status)) return 2;
        if (OrderEntry.STATUS_COMPLETED.equals(status)) return 3;
        return 0;
    }

    public String getFormattedDate() {
        try {
            Date d = new SimpleDateFormat(DB_DATE_FORMAT, Locale.US).parse(createdAt);
            return new SimpleDateFormat("dd MMM yyyy \u2022 hh:mm a", Locale.getDefault()).format(d);
        } catch (ParseException | NullPointerException e) {
            return createdAt == null ? "" : createdAt;
        }
    }

    public String getFormattedDeliveryDate() {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(deliveryDate);
            return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(d);
        } catch (ParseException | NullPointerException e) {
            return deliveryDate == null ? "" : deliveryDate;
        }
    }
}
