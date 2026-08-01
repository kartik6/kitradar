package com.kitradar.api.dto;

import com.kitradar.domain.KitVersion;
import java.math.BigDecimal;
import java.util.List;

public record KitCardDto(
        long id,
        String title,
        String storeSlug,
        String storeName,
        String imageUrl,
        String productUrl,
        KitVersion version,
        BigDecimal sellingPrice,
        BigDecimal mrp,
        int discountPercent,
        boolean inStock,
        List<KitSizeDto> sizes) {
}
