package com.printxpress.printxpress.models;

import com.printxpress.printxpress.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for the product list.
 * Swap the body of {@link #build()} for a network or database source later; callers won't change.
 */
public final class ProductCatalog {

    private static final List<Product> ALL = build();

    private ProductCatalog() {}

    private static List<Product> build() {
        List<Product> l = new ArrayList<>();

        l.add(new Product("Standard Business Card", "Matte Finish", 850.00, 4.5, R.drawable.img_prod_card_standard, "Business Cards", "Matte"));
        l.add(new Product("Premium Business Card", "300gsm Paper", 1250.00, 4.8, R.drawable.img_prod_card_premium, "Business Cards", "Premium"));
        l.add(new Product("Spot UV Business Card", "Premium Finish", 1750.00, 4.9, R.drawable.img_prod_card_uv, "Business Cards", "UV"));
        l.add(new Product("Rounded Corner Card", "Matte Finish", 950.00, 4.7, R.drawable.img_prod_card_rounded, "Business Cards", "Matte"));

        l.add(new Product("Flyer A5", "130gsm Paper", 50.00, 4.6, R.drawable.img_prod_flyer_a5, "Flyers", "Standard"));
        l.add(new Product("Flyer A4", "120gsm Paper", 80.00, 4.7, R.drawable.img_prod_flyer_a4, "Flyers", "Standard"));
        l.add(new Product("Flyer A3", "120gsm Paper", 150.00, 4.8, R.drawable.img_prod_flyer_a3, "Flyers", "Premium"));
        l.add(new Product("DL Flyer", "120gsm Paper", 55.00, 4.5, R.drawable.img_prod_flyer_dl, "Flyers", "Standard"));

        l.add(new Product("Poster A3", "Glossy Paper", 250.00, 4.7, R.drawable.img_prod_poster_a3, "Posters", "Standard"));
        l.add(new Product("Poster A2", "Glossy Paper", 450.00, 4.8, R.drawable.img_prod_poster_a2, "Posters", "Standard"));
        l.add(new Product("Poster A1", "Glossy Paper", 700.00, 4.9, R.drawable.img_prod_poster_a1, "Posters", "Large"));
        l.add(new Product("Large Poster", "Glossy Paper", 950.00, 4.6, R.drawable.img_prod_poster_large, "Posters", "Large"));

        l.add(new Product("Vinyl Banner (2x4 ft)", "Indoor Use", 1200.00, 4.6, R.drawable.img_prod_banner_vinyl, "Banners", "Indoor"));
        l.add(new Product("Flex Banner (4x6 ft)", "Outdoor Use", 1800.00, 4.7, R.drawable.img_prod_banner_flex, "Banners", "Outdoor"));
        l.add(new Product("Roll Up Banner", "Premium Matte", 3200.00, 4.9, R.drawable.img_prod_banner_rollup, "Banners", "RollUp"));

        l.add(new Product("Paper Stickers", "Sheet of 30", 150.00, 4.6, R.drawable.img_prod_sticker_paper, "Stickers", "Paper"));
        l.add(new Product("Vinyl Stickers", "Waterproof Pack", 220.00, 4.7, R.drawable.img_prod_sticker_vinyl, "Stickers", "Vinyl"));
        l.add(new Product("Clear Stickers", "Transparent Finish", 260.00, 4.8, R.drawable.img_prod_sticker_clear, "Stickers", "Clear"));

        l.add(new Product("Cotton T-Shirt", "180 GSM Pure Cotton", 1250.00, 4.7, R.drawable.img_prod_tshirt_cotton, "T-Shirts", "Cotton"));
        l.add(new Product("Premium T-Shirt", "240 GSM Material", 1650.00, 4.8, R.drawable.img_prod_tshirt_premium, "T-Shirts", "Premium"));
        l.add(new Product("Custom Printed Tee", "Sublimation Print", 1450.00, 4.6, R.drawable.img_prod_tshirt_custom, "T-Shirts", "Cotton"));

        l.add(new Product("White Ceramic Mug", "11oz Classic", 850.00, 4.6, R.drawable.img_prod_mug_ceramic, "Mugs", "Ceramic"));
        l.add(new Product("Color Inside Mug", "11oz Two-Tone", 950.00, 4.7, R.drawable.img_prod_mug_color, "Mugs", "Ceramic"));
        l.add(new Product("Magic Heat Mug", "Temperature Sensitive", 1150.00, 4.8, R.drawable.img_prod_mug_magic, "Mugs", "Magic"));

        return Collections.unmodifiableList(l);
    }

    public static List<Product> getAll() {
        return ALL;
    }

    public static List<Product> getByCategory(String category) {
        List<Product> out = new ArrayList<>();
        for (Product p : ALL) {
            if (p.getCategory().equalsIgnoreCase(category)) out.add(p);
        }
        return out;
    }

    public static Product findById(String id) {
        if (id == null) return null;
        for (Product p : ALL) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    /** Category name to a cover image (first product of that category), in catalog order. */
    public static Map<String, Integer> getCategoryCovers() {
        Map<String, Integer> covers = new LinkedHashMap<>();
        for (Product p : ALL) {
            if (!covers.containsKey(p.getCategory())) covers.put(p.getCategory(), p.getImageResource());
        }
        return covers;
    }

    public static List<Product> getTopRated(int limit) {
        List<Product> sorted = new ArrayList<>(ALL);
        Collections.sort(sorted, (a, b) -> Double.compare(b.getRating(), a.getRating()));
        return new ArrayList<>(sorted.subList(0, Math.min(limit, sorted.size())));
    }

    public static List<Product> search(String query) {
        List<Product> out = new ArrayList<>();
        if (query == null) return out;
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return out;
        for (Product p : ALL) {
            String haystack = (p.getName() + " " + p.getSpecDetails() + " "
                    + p.getCategory() + " " + p.getSubTag()).toLowerCase(Locale.ROOT);
            if (haystack.contains(q)) out.add(p);
        }
        return out;
    }
}
