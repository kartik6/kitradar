package com.kitradar.api.dto;

import com.kitradar.domain.Platform;

public record IngestionSourceDto(String slug, String baseUrl, Platform platform) {
}
