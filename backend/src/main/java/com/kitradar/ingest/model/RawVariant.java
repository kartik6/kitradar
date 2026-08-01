package com.kitradar.ingest.model;

import java.math.BigDecimal;

public record RawVariant(
        String externalId,
        String sizeLabel,
        BigDecimal price,
        BigDecimal compareAtPrice,
        boolean available,
        int position) {
}
