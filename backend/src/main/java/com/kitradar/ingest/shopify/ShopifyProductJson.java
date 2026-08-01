package com.kitradar.ingest.shopify;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ShopifyProductJson(
        long id,
        String title,
        String handle,
        List<String> tags,
        @JsonProperty("created_at") String createdAt,
        List<ShopifyImageJson> images,
        List<ShopifyOptionJson> options,
        List<ShopifyVariantJson> variants) {

    public record ShopifyImageJson(String src) {
    }

    public record ShopifyOptionJson(String name, int position) {
    }

    public record ShopifyVariantJson(
            long id,
            String option1,
            String option2,
            String option3,
            String price,
            @JsonProperty("compare_at_price") String compareAtPrice,
            boolean available,
            int position) {
    }
}
