package com.kitradar.search;

import com.kitradar.api.dto.KitPageDto;
import com.kitradar.api.mapper.KitDtoMapper;
import com.kitradar.domain.Product;
import com.kitradar.repository.ProductRepository;
import com.kitradar.repository.StoreRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KitQueryService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final KitDtoMapper mapper;

    public KitQueryService(ProductRepository productRepository, StoreRepository storeRepository, KitDtoMapper mapper) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "kitQueries", key = "#query")
    public KitPageDto search(KitQuery query) {
        validatePriceRange(query.minPrice(), query.maxPrice());
        var result = productRepository.findAll(KitSpecifications.build(query), PageRequest.of(query.page(), query.pageSize(), sort(query.sort())));
        Instant lastUpdatedAt = storeRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(store -> store.getLastScrapedAt())
                .filter(value -> value != null)
                .max(Instant::compareTo)
                .orElse(null);
        return new KitPageDto(
                result.getContent().stream().map(mapper::toCard).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                lastUpdatedAt);
    }

    private Sort sort(SortOption option) {
        return switch (option) {
            case FEATURED -> Sort.by(Sort.Order.desc("inStock"), Sort.Order.desc("sourceCreatedAt"), Sort.Order.desc("id"));
            case PRICE_ASC -> Sort.by(Sort.Order.asc("minPrice"), Sort.Order.desc("id"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("minPrice"), Sort.Order.desc("id"));
            case DISCOUNT -> Sort.by(Sort.Order.desc("discountPercent"), Sort.Order.asc("minPrice"), Sort.Order.desc("id"));
            case NEWEST -> Sort.by(Sort.Order.desc("sourceCreatedAt"), Sort.Order.desc("id"));
        };
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not exceed maxPrice");
        }
    }
}
