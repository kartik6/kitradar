package com.kitradar.repository;

import com.kitradar.domain.Product;
import com.kitradar.domain.ProductKind;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByStoreIdAndExternalId(Long storeId, String externalId);

    long countByStoreIdAndActiveTrue(Long storeId);

    long countByStoreIdAndActiveTrueAndProductKind(Long storeId, ProductKind productKind);

    long countByActiveTrueAndProductKind(ProductKind productKind);

    @Query("select min(product.minPrice) as minPrice, max(product.minPrice) as maxPrice "
            + "from Product product where product.active = true and product.productKind = :kind")
    PriceRange findActivePriceRange(@Param("kind") ProductKind kind);

    @Modifying
    @Query("update Product product set product.active = false "
            + "where product.store.id = :storeId and product.lastSeenAt < :runAt and product.active = true")
    int deactivateNotSeenSince(@Param("storeId") Long storeId, @Param("runAt") java.time.Instant runAt);

    interface PriceRange {
        java.math.BigDecimal getMinPrice();

        java.math.BigDecimal getMaxPrice();
    }
}
