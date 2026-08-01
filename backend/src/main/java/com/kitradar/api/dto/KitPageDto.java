package com.kitradar.api.dto;

import java.time.Instant;
import java.util.List;

public record KitPageDto(
        List<KitCardDto> content,
        int page,
        int pageSize,
        long totalElements,
        int totalPages,
        Instant lastUpdatedAt) {
}
