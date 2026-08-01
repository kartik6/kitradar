package com.kitradar.search;

import java.util.Locale;

public enum SortOption {
    LATEST_ARRIVALS,
    FEATURED,
    PRICE_ASC,
    PRICE_DESC,
    DISCOUNT;

    public static SortOption fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return LATEST_ARRIVALS;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "latest_arrivals", "latest_added", "newest" -> LATEST_ARRIVALS;
            case "featured" -> FEATURED;
            case "price_asc" -> PRICE_ASC;
            case "price_desc" -> PRICE_DESC;
            case "discount" -> DISCOUNT;
            default -> throw new IllegalArgumentException("Unsupported sort option: " + value);
        };
    }
}
