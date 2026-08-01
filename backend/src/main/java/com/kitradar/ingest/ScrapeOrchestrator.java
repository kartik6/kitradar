package com.kitradar.ingest;

import com.kitradar.domain.Store;
import com.kitradar.domain.ScrapeStatus;
import com.kitradar.ingest.sync.CatalogSynchronizer;
import com.kitradar.ingest.sync.SyncResult;
import com.kitradar.repository.StoreRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScrapeOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ScrapeOrchestrator.class);

    private final StoreRepository storeRepository;
    private final ScraperRegistry scraperRegistry;
    private final CatalogSynchronizer catalogSynchronizer;
    private final Clock clock;
    private final CacheManager cacheManager;
    private final AtomicBoolean refreshRunning = new AtomicBoolean();

    public ScrapeOrchestrator(
            StoreRepository storeRepository,
            ScraperRegistry scraperRegistry,
            CatalogSynchronizer catalogSynchronizer,
            Clock clock,
            CacheManager cacheManager) {
        this.storeRepository = storeRepository;
        this.scraperRegistry = scraperRegistry;
        this.catalogSynchronizer = catalogSynchronizer;
        this.clock = clock;
        this.cacheManager = cacheManager;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")
    public void scheduledRefresh() {
        try {
            refresh(null);
        } catch (RefreshAlreadyRunningException exception) {
            log.info("Skipping scheduled refresh because another refresh is still running");
        }
    }

    public RefreshResult refresh(String storeSlug) {
        if (!refreshRunning.compareAndSet(false, true)) {
            throw new RefreshAlreadyRunningException();
        }

        Instant startedAt = clock.instant();
        try {
            List<Store> stores = storesToRefresh(storeSlug);
            List<StoreRefreshResult> results = stores.stream().map(this::refreshStore).toList();
            if (results.stream().anyMatch(result -> result.status() != com.kitradar.domain.ScrapeStatus.FAILED)) {
                clearReadCaches();
            }
            return new RefreshResult(startedAt, clock.instant(), results);
        } finally {
            refreshRunning.set(false);
        }
    }

    private List<Store> storesToRefresh(String storeSlug) {
        if (storeSlug == null || storeSlug.isBlank()) {
            return storeRepository.findByEnabledTrueOrderByNameAsc();
        }
        Store store = storeRepository.findBySlug(storeSlug)
                .filter(Store::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("No enabled store exists for slug " + storeSlug));
        return List.of(store);
    }

    private StoreRefreshResult refreshStore(Store store) {
        try {
            var rawProducts = scraperRegistry.forPlatform(store.getPlatform()).fetch(store);
            SyncResult result = catalogSynchronizer.synchronize(store, rawProducts);
            return new StoreRefreshResult(
                    store.getSlug(),
                    result.status(),
                    result.fetchedCount(),
                    result.upsertedCount(),
                    result.delistedCount(),
                    null);
        } catch (Exception exception) {
            log.warn("Refresh failed for store {}", store.getSlug(), exception);
            catalogSynchronizer.recordFailure(store, exception);
            return new StoreRefreshResult(store.getSlug(), ScrapeStatus.FAILED, 0, 0, 0, exception.getMessage());
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
