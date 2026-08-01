package com.kitradar.api;

import com.kitradar.api.dto.RefreshResultDto;
import com.kitradar.api.dto.RefreshStoreDto;
import com.kitradar.api.dto.IngestionSourceDto;
import com.kitradar.api.dto.ImportFailureDto;
import com.kitradar.api.dto.RawCatalogueDto;
import com.kitradar.api.dto.ScrapeRunDto;
import com.kitradar.api.dto.ScrapeRunPageDto;
import com.kitradar.domain.ScrapeRun;
import com.kitradar.ingest.RefreshAlreadyRunningException;
import com.kitradar.ingest.RefreshResult;
import com.kitradar.ingest.ScrapeOrchestrator;
import com.kitradar.ingest.StoreRefreshResult;
import com.kitradar.ingest.shopify.ShopifyImportService;
import com.kitradar.ingest.shopify.ShopifyProductsResponse;
import com.kitradar.repository.ScrapeRunRepository;
import com.kitradar.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ScrapeOrchestrator orchestrator;
    private final ScrapeRunRepository scrapeRunRepository;
    private final StoreRepository storeRepository;
    private final ShopifyImportService shopifyImportService;

    public AdminController(
            ScrapeOrchestrator orchestrator,
            ScrapeRunRepository scrapeRunRepository,
            StoreRepository storeRepository,
            ShopifyImportService shopifyImportService) {
        this.orchestrator = orchestrator;
        this.scrapeRunRepository = scrapeRunRepository;
        this.storeRepository = storeRepository;
        this.shopifyImportService = shopifyImportService;
    }

    @PostMapping("/refresh")
    public RefreshResultDto refresh(@RequestParam(required = false) String store) {
        try {
            return toDto(orchestrator.refresh(store));
        } catch (RefreshAlreadyRunningException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/imports/shopify")
    public RefreshStoreDto importShopify(
            @RequestParam String store,
            @RequestBody ShopifyProductsResponse catalogue) {
        try {
            return toDto(shopifyImportService.importCatalogue(store, catalogue));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @GetMapping("/sources")
    public java.util.List<IngestionSourceDto> sources() {
        return storeRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(store -> new IngestionSourceDto(store.getSlug(), store.getBaseUrl(), store.getPlatform()))
                .toList();
    }

    @PostMapping("/imports/raw")
    public RefreshStoreDto importRawCatalogue(
            @RequestParam String store,
            @RequestBody RawCatalogueDto catalogue) {
        try {
            return toDto(shopifyImportService.importRawCatalogue(store, catalogue.products()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/imports/failure")
    public void recordImportFailure(
            @RequestParam String store,
            @RequestBody ImportFailureDto failure) {
        try {
            String message = failure.message() == null || failure.message().isBlank()
                    ? "External catalogue worker failed without an error message"
                    : failure.message();
            shopifyImportService.recordExternalFailure(store, message);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @GetMapping("/runs")
    public ScrapeRunPageDto runs(
            @RequestParam(required = false) String store,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and pageSize must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<ScrapeRun> runs = store == null || store.isBlank()
                ? scrapeRunRepository.findAllByOrderByStartedAtDesc(pageable)
                : scrapeRunRepository.findByStoreSlugOrderByStartedAtDesc(store, pageable);
        return new ScrapeRunPageDto(
                runs.map(this::toDto).getContent(),
                runs.getNumber(),
                runs.getSize(),
                runs.getTotalElements(),
                runs.getTotalPages());
    }

    private RefreshResultDto toDto(RefreshResult result) {
        return new RefreshResultDto(
                result.startedAt(),
                result.finishedAt(),
                result.stores().stream()
                        .map(store -> new RefreshStoreDto(
                                store.storeSlug(),
                                store.status(),
                                store.fetchedCount(),
                                store.upsertedCount(),
                                store.delistedCount(),
                                store.errorMessage()))
                        .toList());
    }

    private RefreshStoreDto toDto(StoreRefreshResult result) {
        return new RefreshStoreDto(
                result.storeSlug(),
                result.status(),
                result.fetchedCount(),
                result.upsertedCount(),
                result.delistedCount(),
                result.errorMessage());
    }

    private ScrapeRunDto toDto(ScrapeRun run) {
        return new ScrapeRunDto(
                run.getId(),
                run.getStore().getSlug(),
                run.getStore().getName(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getStatus(),
                run.getFetchedCount(),
                run.getUpsertedCount(),
                run.getDelistedCount(),
                run.getErrorMessage());
    }
}
