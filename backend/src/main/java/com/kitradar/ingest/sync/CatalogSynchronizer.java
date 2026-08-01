package com.kitradar.ingest.sync;

import com.kitradar.domain.Product;
import com.kitradar.domain.ScrapeRun;
import com.kitradar.domain.ScrapeStatus;
import com.kitradar.domain.Store;
import com.kitradar.ingest.model.RawProduct;
import com.kitradar.normalize.ProductNormalizer;
import com.kitradar.repository.ProductRepository;
import com.kitradar.repository.ScrapeRunRepository;
import com.kitradar.repository.StoreRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class CatalogSynchronizer {

    private final ProductRepository productRepository;
    private final ScrapeRunRepository scrapeRunRepository;
    private final StoreRepository storeRepository;
    private final ProductNormalizer normalizer;
    private final Clock clock;

    public CatalogSynchronizer(
            ProductRepository productRepository,
            ScrapeRunRepository scrapeRunRepository,
            StoreRepository storeRepository,
            ProductNormalizer normalizer,
            Clock clock) {
        this.productRepository = productRepository;
        this.scrapeRunRepository = scrapeRunRepository;
        this.storeRepository = storeRepository;
        this.normalizer = normalizer;
        this.clock = clock;
    }

    @Transactional
    public SyncResult synchronize(Store store, List<RawProduct> rawProducts) {
        Instant runAt = clock.instant();
        long previousSuccessfulCount = productRepository.countByStoreIdAndActiveTrue(store.getId());
        boolean partial = rawProducts.size() < previousSuccessfulCount * 0.5;
        ScrapeRun run = scrapeRunRepository.save(ScrapeRun.start(store, runAt));

        for (RawProduct rawProduct : rawProducts) {
            var normalized = normalizer.normalize(rawProduct);
            Product product = productRepository.findByStoreIdAndExternalId(store.getId(), normalized.externalId())
                    .map(existing -> {
                        existing.apply(normalized, runAt);
                        return existing;
                    })
                    .orElseGet(() -> new Product(store, normalized, runAt));
            productRepository.save(product);
        }

        int delistedCount = partial ? 0 : productRepository.deactivateNotSeenSince(store.getId(), runAt);
        ScrapeStatus status = partial ? ScrapeStatus.PARTIAL : ScrapeStatus.SUCCESS;
        run.finish(status, clock.instant(), rawProducts.size(), rawProducts.size(), delistedCount);
        store.markScraped(runAt);
        storeRepository.save(store);
        return new SyncResult(status, rawProducts.size(), rawProducts.size(), delistedCount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Store store, Exception exception) {
        Instant finishedAt = clock.instant();
        ScrapeRun run = ScrapeRun.start(store, finishedAt);
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        run.fail(finishedAt, message.length() <= 1_000 ? message : message.substring(0, 1_000));
        scrapeRunRepository.save(run);
    }
}
