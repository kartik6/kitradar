package com.kitradar.ingest.shopify;

import com.kitradar.domain.Platform;
import com.kitradar.domain.Store;
import com.kitradar.ingest.AbstractStoreScraper;
import com.kitradar.ingest.HostRequestLimiter;
import com.kitradar.ingest.StoreScraper;
import com.kitradar.ingest.model.RawProduct;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ShopifyScraper extends AbstractStoreScraper implements StoreScraper {

    private static final int PAGE_SIZE = 250;
    private static final int MAX_PAGES = 40;

    private final ShopifyMapper mapper;

    public ShopifyScraper(RestClient storeRestClient, HostRequestLimiter limiter, ShopifyMapper mapper) {
        super(storeRestClient, limiter);
        this.mapper = mapper;
    }

    @Override
    public Platform platform() {
        return Platform.SHOPIFY;
    }

    @Override
    public List<RawProduct> fetch(Store store) {
        List<RawProduct> products = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            ShopifyProductsResponse response = getJson(productsUri(store.getBaseUrl(), page), ShopifyProductsResponse.class);
            products.addAll(response.products().stream().map(product -> mapper.map(product, store.getBaseUrl())).toList());
            if (response.products().size() < PAGE_SIZE) {
                return List.copyOf(products);
            }
        }
        throw new IllegalStateException("Shopify pagination exceeded " + MAX_PAGES + " pages for " + store.getSlug());
    }

    private URI productsUri(String baseUrl, int page) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/products.json")
                .queryParam("limit", PAGE_SIZE)
                .queryParam("page", page)
                .build(true)
                .toUri();
    }
}
