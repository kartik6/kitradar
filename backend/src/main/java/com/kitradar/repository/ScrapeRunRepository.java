package com.kitradar.repository;

import com.kitradar.domain.ScrapeRun;
import com.kitradar.domain.ScrapeStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {

    List<ScrapeRun> findByStatus(ScrapeStatus status);

    @EntityGraph(attributePaths = "store")
    Page<ScrapeRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "store")
    Page<ScrapeRun> findByStoreSlugOrderByStartedAtDesc(String storeSlug, Pageable pageable);
}
