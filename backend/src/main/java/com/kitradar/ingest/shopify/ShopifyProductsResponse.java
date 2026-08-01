package com.kitradar.ingest.shopify;

import java.util.List;

public record ShopifyProductsResponse(List<ShopifyProductJson> products) {

    public ShopifyProductsResponse {
        products = products == null ? List.of() : List.copyOf(products);
    }
}
