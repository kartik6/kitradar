package com.kitradar.ingest.shopify;

import com.kitradar.domain.Platform;
import com.kitradar.domain.Store;
import com.kitradar.ingest.StoreRefreshResult;
import com.kitradar.ingest.model.RawProduct;
import com.kitradar.ingest.sync.CatalogSynchronizer;
import com.kitradar.ingest.sync.SyncResult;
import com.kitradar.repository.StoreRepository;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Imports a complete Shopify catalogue fetched by a trusted external runner.
 *
 * <p>The public store request happens outside the API host when a merchant rate-limits that host.
 * Product mapping and database synchronization still happen here, so all catalogue writes follow
 * the normal validation, normalization, run-history, and cache-invalidation path.</p>
 */
@Service
public class ShopifyImportService {

    private final StoreRepository storeRepository;
    private final ShopifyMapper mapper;
    private final CatalogSynchronizer catalogSynchronizer;
    private final CacheManager cacheManager;

    public ShopifyImportService(
            StoreRepository storeRepository,
            ShopifyMapper mapper,
            CatalogSynchronizer catalogSynchronizer,
            CacheManager cacheManager) {
        this.storeRepository = storeRepository;
        this.mapper = mapper;
        this.catalogSynchronizer = catalogSynchronizer;
        this.cacheManager = cacheManager;
    }

    public StoreRefreshResult importCatalogue(String storeSlug, ShopifyProductsResponse response) {
        Store store = store(storeSlug, Platform.SHOPIFY);
        List<RawProduct> products = response.products().stream()
                .map(product -> mapper.map(product, store.getBaseUrl()))
                .toList();
        return importProducts(store, products);
    }

    public StoreRefreshResult importRawCatalogue(String storeSlug, List<RawProduct> products) {
        return importProducts(store(storeSlug, null), products);
    }

    public StoreRefreshResult importPartialCatalogue(String storeSlug, List<RawProduct> products) {
        return importProducts(store(storeSlug, null), products, false);
    }

    public void recordExternalFailure(String storeSlug, String message) {
        Store store = store(storeSlug, null);
        catalogSynchronizer.recordFailure(store, new IllegalStateException(message));
    }

    private Store store(String storeSlug, Platform requiredPlatform) {
        return storeRepository.findBySlug(storeSlug)
                .filter(Store::isEnabled)
                .filter(candidate -> requiredPlatform == null || candidate.getPlatform() == requiredPlatform)
                .orElseThrow(() -> new IllegalArgumentException("No enabled "
                        + (requiredPlatform == null ? "" : requiredPlatform + " ")
                        + "store exists for slug " + storeSlug));
    }

    private StoreRefreshResult importProducts(Store store, List<RawProduct> products) {
        return importProducts(store, products, true);
    }

    private StoreRefreshResult importProducts(Store store, List<RawProduct> products, boolean complete) {
        try {
            SyncResult result = catalogSynchronizer.synchronize(store, products, complete);
            clearReadCaches();
            return new StoreRefreshResult(
                    store.getSlug(), result.status(), result.fetchedCount(), result.upsertedCount(), result.delistedCount(), null);
        } catch (RuntimeException exception) {
            catalogSynchronizer.recordFailure(store, exception);
            throw exception;
        }
    }

    private void clearReadCaches() {
        for (String cacheName : List.of("kitQueries", "meta")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
