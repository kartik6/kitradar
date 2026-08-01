package com.kitradar.api.dto;

import com.kitradar.domain.KitVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MetaDto(
        long totalKits,
        long storeCount,
        Instant lastUpdatedAt,
        List<String> sizes,
        List<KitVersion> versions,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
}
