package com.kitradar.ingest.dotpe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitradar.domain.Platform;
import com.kitradar.domain.Store;
import com.kitradar.ingest.AbstractStoreScraper;
import com.kitradar.ingest.HostRequestLimiter;
import com.kitradar.ingest.ScrapeException;
import com.kitradar.ingest.StoreScraper;
import com.kitradar.ingest.model.RawProduct;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class DotPeScraper extends AbstractStoreScraper implements StoreScraper {

    private final BuildIdResolver buildIdResolver;
    private final SitemapProductParser sitemapProductParser;
    private final DotPeMapper mapper;
    private final ObjectMapper objectMapper;

    public DotPeScraper(
            RestClient storeRestClient,
            HostRequestLimiter limiter,
            BuildIdResolver buildIdResolver,
            SitemapProductParser sitemapProductParser,
            DotPeMapper mapper,
            ObjectMapper objectMapper) {
        super(storeRestClient, limiter);
        this.buildIdResolver = buildIdResolver;
        this.sitemapProductParser = sitemapProductParser;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Platform platform() {
        return Platform.DOTPE;
    }

    @Override
    public List<RawProduct> fetch(Store store) {
        String buildId = buildIdResolver.resolve(store.getBaseUrl());
        List<SitemapProductParser.ProductLocation> locations = sitemapProductParser.parse(
                getText(URI.create(store.getBaseUrl() + "/sitemap_products.xml")));
        return locations.stream()
                .map(location -> fetchProduct(store, buildId, location, true))
                .filter(Objects::nonNull)
                .toList();
    }

    private RawProduct fetchProduct(
            Store store,
            String buildId,
            SitemapProductParser.ProductLocation location,
            boolean refreshBuildIdOnNotFound) {
        try {
            DotPeProductsResponse response = getProduct(productUri(store.getBaseUrl(), buildId, location));
            return mapper.map(response.pageProps().product(), store.getBaseUrl());
        } catch (ScrapeException exception) {
            if (refreshBuildIdOnNotFound && isNotFound(exception)) {
                try {
                    return fetchProduct(store, buildIdResolver.resolve(store.getBaseUrl()), location, false);
                } catch (ScrapeException retryException) {
                    if (isNotFound(retryException)) {
                        return null;
                    }
                    throw retryException;
                }
            }
            if (isNotFound(exception)) {
                return null;
            }
            throw exception;
        }
    }

    private boolean isNotFound(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof RestClientResponseException responseException
                    && responseException.getStatusCode().value() == 404) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private URI productUri(String baseUrl, String buildId, SitemapProductParser.ProductLocation location) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("_next", "data", buildId, "product", location.id(), location.slug() + ".json")
                .build(true)
                .toUri();
    }

    private DotPeProductsResponse getProduct(URI uri) {
        try {
            return objectMapper.readValue(getText(uri), DotPeProductsResponse.class);
        } catch (JsonProcessingException exception) {
            throw new ScrapeException("Could not parse DotPe product response from " + uri, exception);
        }
    }
}
