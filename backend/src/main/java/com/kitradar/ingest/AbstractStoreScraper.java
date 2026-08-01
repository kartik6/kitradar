package com.kitradar.ingest;

import java.net.URI;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public abstract class AbstractStoreScraper {

    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;
    private final HostRequestLimiter limiter;

    protected AbstractStoreScraper(RestClient restClient, HostRequestLimiter limiter) {
        this.restClient = restClient;
        this.limiter = limiter;
    }

    protected <T> T getJson(URI uri, Class<T> responseType) {
        return get(uri, () -> restClient.get().uri(uri).retrieve().body(responseType));
    }

    protected String getText(URI uri) {
        return get(uri, () -> restClient.get().uri(uri).retrieve().body(String.class));
    }

    private <T> T get(URI uri, HostRequestLimiter.ThrowingSupplier<T> request) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return limiter.execute(uri, request);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ScrapeException("Interrupted while fetching " + uri, exception);
            } catch (Exception exception) {
                if (exception instanceof RuntimeException runtimeException) {
                    lastFailure = runtimeException;
                } else {
                    lastFailure = new ScrapeException("Failed to fetch " + uri, exception);
                }
                if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
                    break;
                }
                backoff(attempt);
            }
        }
        String detail = lastFailure == null || lastFailure.getMessage() == null
                ? "unknown error"
                : lastFailure.getMessage();
        throw new ScrapeException(
                "Failed to fetch " + uri + " after " + MAX_ATTEMPTS + " attempts: " + detail,
                lastFailure);
    }

    private boolean isRetryable(Exception exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        return exception instanceof RestClientResponseException responseException
                && responseException.getStatusCode().is5xxServerError();
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(250L * (1L << (attempt - 1)));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ScrapeException("Interrupted during retry backoff", exception);
        }
    }
}
