package com.kitradar.ingest;

import java.time.Instant;
import java.util.List;

public record RefreshResult(Instant startedAt, Instant finishedAt, List<StoreRefreshResult> stores) {
}
