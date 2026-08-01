package com.kitradar.api.dto;

import com.kitradar.domain.ScrapeStatus;
import java.time.Instant;

public record ScrapeRunDto(
        long id,
        String storeSlug,
        String storeName,
        Instant startedAt,
        Instant finishedAt,
        ScrapeStatus status,
        int fetchedCount,
        int upsertedCount,
        int delistedCount,
        String errorMessage) {
}
