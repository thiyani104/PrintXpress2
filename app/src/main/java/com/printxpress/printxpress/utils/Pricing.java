package com.printxpress.printxpress.utils;

import com.printxpress.printxpress.models.CartItem;
import com.printxpress.printxpress.models.Offer;
import com.printxpress.printxpress.models.OfferCatalog;

import java.util.List;

/** All price maths in one place so the cart, checkout and order history always agree. */
public final class Pricing {

    public static final double HOME_DELIVERY_FEE = 250.0;

    public static final int OK = 0;
    public static final int ERR_INVALID = 1;
    public static final int ERR_EXPIRED = 2;
    public static final int ERR_NOT_APPLICABLE = 3;
    public static final int ERR_MIN_SUBTOTAL = 4;

    private Pricing() {}

    public static class Quote {
        public double subtotal;
        public double discount;
        public double deliveryFee;
        public double total;
        /** The code that was successfully applied, or null. */
        public String appliedCode;
        public int promoError = OK;
        /** Set when the code is rejected for a minimum spend; used for the message. */
        public double minSubtotal;
        public boolean freeDeliveryApplied;
    }

    public static Quote quote(List<CartItem> items, String promoCode, boolean homeDelivery) {
        Quote q = new Quote();
        for (CartItem item : items) q.subtotal += item.getLineTotal();

        double fee = homeDelivery ? HOME_DELIVERY_FEE : 0;

        if (promoCode != null && !promoCode.trim().isEmpty()) {
            Offer offer = OfferCatalog.find(promoCode);
            if (offer == null) {
                q.promoError = ERR_INVALID;
            } else if (!offer.isActive()) {
                q.promoError = ERR_EXPIRED;
            } else {
                switch (offer.getType()) {
                    case CATEGORY_PERCENT: {
                        double eligible = 0;
                        for (CartItem item : items) {
                            if (offer.getCategory() != null
                                    && offer.getCategory().equalsIgnoreCase(item.getCategory())) {
                                eligible += item.getLineTotal();
                            }
                        }
                        if (eligible <= 0) q.promoError = ERR_NOT_APPLICABLE;
                        else q.discount = eligible * offer.getPercent() / 100.0;
                        break;
                    }
                    case ORDER_PERCENT:
                        if (q.subtotal < offer.getMinSubtotal()) {
                            q.promoError = ERR_MIN_SUBTOTAL;
                            q.minSubtotal = offer.getMinSubtotal();
                        } else {
                            q.discount = q.subtotal * offer.getPercent() / 100.0;
                        }
                        break;
                    case FREE_DELIVERY:
                        if (q.subtotal < offer.getMinSubtotal()) {
                            q.promoError = ERR_MIN_SUBTOTAL;
                            q.minSubtotal = offer.getMinSubtotal();
                        } else {
                            q.freeDeliveryApplied = true;
                            fee = 0;
                        }
                        break;
                    default:
                        break;
                }
                if (q.promoError == OK) q.appliedCode = offer.getCode();
            }
        }

        q.discount = Math.round(q.discount * 100.0) / 100.0;
        q.deliveryFee = fee;
        q.total = Math.max(0, q.subtotal - q.discount + q.deliveryFee);
        return q;
    }
}
