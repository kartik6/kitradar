package com.kitradar.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("kitQueries", "meta");
        manager.setCaffeine(Caffeine.newBuilder().recordStats().maximumSize(500).expireAfterWrite(Duration.ofMinutes(15)));
        return manager;
    }
}
