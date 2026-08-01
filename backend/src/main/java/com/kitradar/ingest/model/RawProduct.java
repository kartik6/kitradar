package com.kitradar.ingest.model;

import java.time.Instant;
import java.util.List;

public record RawProduct(
        String externalId,
        String title,
        String productUrl,
        String imageUrl,
        List<String> tags,
        String category,
        Instant sourceCreatedAt,
        List<RawVariant> variants) {

    public RawProduct {
        tags = tags == null ? List.of() : List.copyOf(tags);
        variants = variants == null ? List.of() : List.copyOf(variants);
    }
}
