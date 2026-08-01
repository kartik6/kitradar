package com.kitradar.repository;

import com.kitradar.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByStoreIdAndExternalId(Long storeId, String externalId);

    long countByStoreIdAndActiveTrue(Long storeId);

    @Modifying
    @Query("update Product product set product.active = false "
            + "where product.store.id = :storeId and product.lastSeenAt < :runAt and product.active = true")
    int deactivateNotSeenSince(@Param("storeId") Long storeId, @Param("runAt") java.time.Instant runAt);
}
