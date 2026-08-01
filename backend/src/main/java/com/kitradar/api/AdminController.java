package com.kitradar.api;

import com.kitradar.api.dto.RefreshResultDto;
import com.kitradar.api.dto.RefreshStoreDto;
import com.kitradar.ingest.RefreshAlreadyRunningException;
import com.kitradar.ingest.RefreshResult;
import com.kitradar.ingest.ScrapeOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ScrapeOrchestrator orchestrator;

    public AdminController(ScrapeOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
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
}
