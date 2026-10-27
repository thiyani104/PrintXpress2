package com.printxpress.printxpress.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Offer {

    public enum Type { CATEGORY_PERCENT, ORDER_PERCENT, FREE_DELIVERY }

    private final String code;
    private final String title;
    private final String description;
    private final String validUntil; // yyyy-MM-dd
    private final Type type;
    private final double percent;
    private final String category;   // only for CATEGORY_PERCENT
    private final double minSubtotal;
    private final int imageRes;

    public Offer(String code, String title, String description, String validUntil, Type type,
                 double percent, String category, double minSubtotal, int imageRes) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.validUntil = validUntil;
        this.type = type;
        this.percent = percent;
        this.category = category;
        this.minSubtotal = minSubtotal;
        this.imageRes = imageRes;
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getValidUntil() { return validUntil; }
    public Type getType() { return type; }
    public double getPercent() { return percent; }
    public String getCategory() { return category; }
    public double getMinSubtotal() { return minSubtotal; }
    public int getImageRes() { return imageRes; }

    private Date parseValidUntil() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(validUntil);
        } catch (ParseException | NullPointerException e) {
            return null;
        }
    }

    /** Valid through the end of the validUntil day. */
    public boolean isActive() {
        Date end = parseValidUntil();
        if (end == null) return true;
        Calendar c = Calendar.getInstance();
        c.setTime(end);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        return System.currentTimeMillis() <= c.getTimeInMillis();
    }

    public String getFormattedValidUntil() {
        Date end = parseValidUntil();
        return end == null ? "" : new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(end);
    }
}
