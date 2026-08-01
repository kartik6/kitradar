package com.kitradar.ingest;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public abstract class AbstractStoreScraper {

    private static final int MAX_ATTEMPTS = 4;

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
                backoff(attempt, exception);
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
                && (responseException.getStatusCode().value() == 429 || responseException.getStatusCode().is5xxServerError());
    }

    private void backoff(int attempt, Exception exception) {
        long delayMillis = retryAfterMillis(exception)
                .orElse(Math.min(30_000L, 2_000L * (1L << (attempt - 1))));
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ScrapeException("Interrupted during retry backoff", interruptedException);
        }
    }

    private java.util.Optional<Long> retryAfterMillis(Exception exception) {
        if (!(exception instanceof RestClientResponseException responseException)) {
            return java.util.Optional.empty();
        }
        String retryAfter = responseException.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter == null || retryAfter.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Math.max(0L, Long.parseLong(retryAfter.trim()) * 1_000L));
        } catch (NumberFormatException ignored) {
            try {
                long delay = ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().toEpochMilli() - System.currentTimeMillis();
                return java.util.Optional.of(Math.max(0L, delay));
            } catch (RuntimeException invalidDate) {
                return java.util.Optional.empty();
            }
        }
    }
}
