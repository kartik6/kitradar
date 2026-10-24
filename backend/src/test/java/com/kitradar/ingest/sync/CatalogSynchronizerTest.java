package com.kitradar.ingest.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.kitradar.domain.*;
import com.kitradar.ingest.model.RawProduct;
import com.kitradar.normalize.ProductNormalizer;
import com.kitradar.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogSynchronizerTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final ScrapeRunRepository runs = mock(ScrapeRunRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final ProductNormalizer normalizer = mock(ProductNormalizer.class);
    private final Store store = mock(Store.class);
    private final Instant now = Instant.parse("2026-10-24T00:00:00Z");
    private final CatalogSynchronizer synchronizer = new CatalogSynchronizer(
            products, runs, stores, normalizer, Clock.fixed(now, ZoneOffset.UTC));

    private List<RawProduct> catalogue(long previousCount) {
        when(store.getId()).thenReturn(1L);
        when(products.countByStoreIdAndActiveTrue(1L)).thenReturn(previousCount);
        when(runs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.findByStoreIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(normalizer.normalize(any())).thenReturn(new ProductData(
                "1", "Jersey", "jersey", "https://example.test/1", null,
                KitVersion.FAN, ProductKind.JERSEY, BigDecimal.TEN, null, 0, true, null, List.of()));
        return List.of(new RawProduct("1", "Jersey", "https://example.test/1", null, List.of(), null, null, List.of()));
    }

    @Test
    void explicitPartialAboveShrinkThresholdUpsertsButNeverDelists() {
        var input = catalogue(1);
        var result = synchronizer.synchronize(store, input, false);
        assertThat(result.status()).isEqualTo(ScrapeStatus.PARTIAL);
        assertThat(result.upsertedCount()).isEqualTo(1);
        assertThat(result.delistedCount()).isZero();
        verify(products).save(any(Product.class));
        verify(products, never()).deactivateNotSeenSince(any(), any());
        verify(runs).save(argThat(run -> run.getStatus() == ScrapeStatus.PARTIAL));
    }

    @Test
    void completeCatalogueStillDelistsMissingProducts() {
        var input = catalogue(1);
        when(products.deactivateNotSeenSince(1L, now)).thenReturn(2);
        var result = synchronizer.synchronize(store, input);
        assertThat(result.status()).isEqualTo(ScrapeStatus.SUCCESS);
        assertThat(result.delistedCount()).isEqualTo(2);
        verify(products).deactivateNotSeenSince(1L, now);
    }

    @Test
    void completeButSuspiciouslySmallCatalogueKeepsExistingGuard() {
        var result = synchronizer.synchronize(store, catalogue(10), true);
        assertThat(result.status()).isEqualTo(ScrapeStatus.PARTIAL);
        verify(products, never()).deactivateNotSeenSince(any(), any());
    }

    @Test
    void emptyCatalogueNeverDelistsEvenWithoutPreviousProducts() {
        catalogue(0);
        var result = synchronizer.synchronize(store, List.of(), true);
        assertThat(result.status()).isEqualTo(ScrapeStatus.PARTIAL);
        verify(products, never()).deactivateNotSeenSince(any(), any());
    }
}
