package com.kitradar.ingest;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;
import org.springframework.stereotype.Component;

@Component
public class HostRequestLimiter {

    private static final long MIN_GAP_NANOS = Duration.ofMillis(250).toNanos();
    private static final int MAX_CONCURRENT_REQUESTS = 4;

    private final ConcurrentHashMap<String, HostState> hosts = new ConcurrentHashMap<>();

    public <T> T execute(URI uri, ThrowingSupplier<T> request) throws Exception {
        HostState state = hosts.computeIfAbsent(uri.getHost(), unused -> new HostState());
        state.permits.acquire();
        try {
            waitForTurn(state);
            return request.get();
        } finally {
            state.permits.release();
        }
    }

    private void waitForTurn(HostState state) {
        synchronized (state) {
            long now = System.nanoTime();
            long waitNanos = state.nextRequestAtNanos - now;
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
            }
            state.nextRequestAtNanos = System.nanoTime() + MIN_GAP_NANOS;
        }
    }

    private static final class HostState {
        private final Semaphore permits = new Semaphore(MAX_CONCURRENT_REQUESTS);
        private long nextRequestAtNanos;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
