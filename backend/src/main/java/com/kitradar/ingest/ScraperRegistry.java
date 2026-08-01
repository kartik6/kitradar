package com.kitradar.ingest;

import com.kitradar.domain.Platform;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ScraperRegistry {

    private final Map<Platform, StoreScraper> scrapers;

    public ScraperRegistry(List<StoreScraper> scrapers) {
        this.scrapers = scrapers.stream().collect(Collectors.toUnmodifiableMap(
                StoreScraper::platform,
                Function.identity(),
                (first, second) -> {
                    throw new IllegalStateException("Multiple scrapers registered for " + first.platform());
                }));
    }

    public StoreScraper forPlatform(Platform platform) {
        StoreScraper scraper = scrapers.get(platform);
        if (scraper == null) {
            throw new IllegalArgumentException("No scraper registered for platform " + platform);
        }
        return scraper;
    }
}
