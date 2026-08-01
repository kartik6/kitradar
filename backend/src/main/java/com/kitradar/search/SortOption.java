package com.kitradar.search;

import java.util.Locale;

public enum SortOption {
    LATEST_ADDED,
    FEATURED,
    PRICE_ASC,
    PRICE_DESC,
    DISCOUNT,
    NEWEST;

    public static SortOption fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return LATEST_ADDED;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "latest_added" -> LATEST_ADDED;
            case "featured" -> FEATURED;
            case "price_asc" -> PRICE_ASC;
            case "price_desc" -> PRICE_DESC;
            case "discount" -> DISCOUNT;
            case "newest" -> NEWEST;
            default -> throw new IllegalArgumentException("Unsupported sort option: " + value);
        };
    }
}
