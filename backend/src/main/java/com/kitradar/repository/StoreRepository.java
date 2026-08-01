package com.kitradar.repository;

import com.kitradar.domain.Store;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByEnabledTrueOrderByNameAsc();

    Optional<Store> findBySlug(String slug);
}
