package com.kitradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scrape_run")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScrapeRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScrapeStatus status;

    @Column(name = "fetched_count", nullable = false)
    private int fetchedCount;

    @Column(name = "upserted_count", nullable = false)
    private int upsertedCount;

    @Column(name = "delisted_count", nullable = false)
    private int delistedCount;

    @Column(name = "error_message")
    private String errorMessage;

    public static ScrapeRun start(Store store, Instant startedAt) {
        ScrapeRun run = new ScrapeRun();
        run.store = store;
        run.startedAt = startedAt;
        run.status = ScrapeStatus.RUNNING;
        return run;
    }

    public void finish(ScrapeStatus status, Instant finishedAt, int fetchedCount, int upsertedCount, int delistedCount) {
        this.status = status;
        this.finishedAt = finishedAt;
        this.fetchedCount = fetchedCount;
        this.upsertedCount = upsertedCount;
        this.delistedCount = delistedCount;
    }

    public void fail(Instant finishedAt, String errorMessage) {
        this.status = ScrapeStatus.FAILED;
        this.finishedAt = finishedAt;
        this.errorMessage = errorMessage;
    }
}
