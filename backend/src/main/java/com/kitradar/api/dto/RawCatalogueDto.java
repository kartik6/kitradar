package com.kitradar.api.dto;

import com.kitradar.ingest.model.RawProduct;
import java.util.List;

public record RawCatalogueDto(List<RawProduct> products) {

    public RawCatalogueDto {
        products = products == null ? List.of() : List.copyOf(products);
    }
}
