package com.kitradar.api.dto;

import java.time.Instant;
import java.util.List;

public record RefreshResultDto(Instant startedAt, Instant finishedAt, List<RefreshStoreDto> stores) {
}
