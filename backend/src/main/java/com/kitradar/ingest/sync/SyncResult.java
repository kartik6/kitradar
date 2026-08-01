package com.kitradar.ingest.sync;

import com.kitradar.domain.ScrapeStatus;

public record SyncResult(ScrapeStatus status, int fetchedCount, int upsertedCount, int delistedCount) {
}
