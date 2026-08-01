package com.kitradar.api.dto;

import java.util.List;

public record ScrapeRunPageDto(
        List<ScrapeRunDto> content,
        int page,
        int pageSize,
        long totalElements,
        int totalPages) {
}
