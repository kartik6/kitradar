package com.kitradar.repository;

import com.kitradar.domain.ScrapeRun;
import com.kitradar.domain.ScrapeStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {

    List<ScrapeRun> findByStatus(ScrapeStatus status);
}
