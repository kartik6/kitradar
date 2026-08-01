package com.kitradar.api.dto;

import com.kitradar.domain.ScrapeStatus;

public record RefreshStoreDto(
        String storeSlug,
        ScrapeStatus status,
        int fetchedCount,
        int upsertedCount,
        int delistedCount,
        String errorMessage) {
}
