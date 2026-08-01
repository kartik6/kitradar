package com.kitradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_scraped_at")
    private Instant lastScrapedAt;

    public Store(String slug, String name, String baseUrl, Platform platform) {
        this.slug = slug;
        this.name = name;
        this.baseUrl = baseUrl;
        this.platform = platform;
    }

    public void markScraped(Instant scrapedAt) {
        this.lastScrapedAt = scrapedAt;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
