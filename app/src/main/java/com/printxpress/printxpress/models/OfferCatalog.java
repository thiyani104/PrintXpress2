package com.printxpress.printxpress.models;

import com.printxpress.printxpress.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Promotions shown on the Offers screen and accepted as promo codes in the cart.
 * Update the validUntil dates here whenever a campaign changes.
 */
public final class OfferCatalog {

    private static final List<Offer> ALL = build();

    private OfferCatalog() {}

    private static List<Offer> build() {
        List<Offer> l = new ArrayList<>();
        l.add(new Offer("CARD20", "20% OFF", "On Business Cards", "2026-12-31",
                Offer.Type.CATEGORY_PERCENT, 20, "Business Cards", 0, R.drawable.img_promo_cards));
        l.add(new Offer("SAVE15", "15% OFF", "On orders above Rs. 5,000", "2026-12-31",
                Offer.Type.ORDER_PERCENT, 15, null, 5000, R.drawable.img_promo_bulk));
        l.add(new Offer("FREE3000", "Free Delivery", "On orders above Rs. 3,000", "2026-12-31",
                Offer.Type.FREE_DELIVERY, 0, null, 3000, R.drawable.img_promo_delivery));
        return Collections.unmodifiableList(l);
    }

    public static List<Offer> getAll() {
        return ALL;
    }

    public static Offer find(String code) {
        if (code == null) return null;
        String c = code.trim();
        for (Offer o : ALL) {
            if (o.getCode().equalsIgnoreCase(c)) return o;
        }
        return null;
    }
}
