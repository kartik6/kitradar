package com.kitradar.ingest;

import com.kitradar.domain.ScrapeStatus;

public record StoreRefreshResult(
        String storeSlug,
        ScrapeStatus status,
        int fetchedCount,
        int upsertedCount,
        int delistedCount,
        String errorMessage) {
}
