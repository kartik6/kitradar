package com.kitradar.ingest.dotpe;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record DotPeProductJson(
        long id,
        String name,
        BigDecimal price,
        @JsonProperty("discounted_price") BigDecimal discountedPrice,
        @JsonProperty("image_url") String imageUrl,
        String link,
        DotPeCategoryJson category,
        List<DotPeCollectionJson> collections,
        List<DotPeVariantJson> variants) {

    public record DotPeCategoryJson(String name) {
    }

    public record DotPeCollectionJson(String name) {
    }

    public record DotPeVariantJson(
            @JsonProperty("variant_id") long variantId,
            @JsonProperty("variant_name") String variantName,
            int available,
            BigDecimal price,
            @JsonProperty("discounted_price") BigDecimal discountedPrice,
            @JsonProperty("option_variant_names") List<DotPeOptionValueJson> optionVariantNames) {
    }

    public record DotPeOptionValueJson(
            @JsonProperty("option_name") String optionName,
            @JsonProperty("variant_name") String variantName) {
    }
}
