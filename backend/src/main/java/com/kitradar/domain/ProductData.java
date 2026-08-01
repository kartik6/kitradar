package com.kitradar.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductData(
        String externalId,
        String title,
        String searchText,
        String productUrl,
        String imageUrl,
        KitVersion version,
        ProductKind productKind,
        BigDecimal minPrice,
        BigDecimal compareAtPrice,
        int discountPercent,
        boolean inStock,
        Instant sourceCreatedAt,
        List<VariantData> variants) {

    public ProductData {
        variants = List.copyOf(variants);
    }

    public record VariantData(
            String externalId,
            String sizeLabel,
            SizeNorm sizeNorm,
            BigDecimal price,
            BigDecimal compareAtPrice,
            boolean available,
            int position) {
    }
}
