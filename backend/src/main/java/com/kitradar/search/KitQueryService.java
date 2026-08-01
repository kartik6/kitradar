package com.kitradar.search;

import com.kitradar.api.dto.KitPageDto;
import com.kitradar.api.mapper.KitDtoMapper;
import com.kitradar.domain.Product;
import com.kitradar.repository.ProductRepository;
import com.kitradar.repository.StoreRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private final Clock clock;

    public KitQueryService(
            ProductRepository productRepository,
            StoreRepository storeRepository,
            KitDtoMapper mapper,
            Clock clock) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "kitQueries", key = "#query")
    public KitPageDto search(KitQuery query) {
        validatePriceRange(query.minPrice(), query.maxPrice());
        List<Product> content;
        long totalElements;
        int totalPages;
        if (query.sort() == SortOption.FEATURED) {
            List<Product> ordered = featuredOrder(productRepository.findAll(KitSpecifications.build(query), featuredBaseSort()), query);
            totalElements = ordered.size();
            totalPages = totalPages(totalElements, query.pageSize());
            int fromIndex = Math.min(query.page() * query.pageSize(), ordered.size());
            int toIndex = Math.min(fromIndex + query.pageSize(), ordered.size());
            content = ordered.subList(fromIndex, toIndex);
        } else {
            var result = productRepository.findAll(KitSpecifications.build(query), PageRequest.of(query.page(), query.pageSize(), sort(query.sort())));
            content = result.getContent();
            totalElements = result.getTotalElements();
            totalPages = result.getTotalPages();
        }
        Instant lastUpdatedAt = storeRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(store -> store.getLastScrapedAt())
                .filter(value -> value != null)
                .max(Instant::compareTo)
                .orElse(null);
        return new KitPageDto(
                content.stream().map(mapper::toCard).toList(),
                query.page(),
                query.pageSize(),
                totalElements,
                totalPages,
                lastUpdatedAt);
    }

    private Sort sort(SortOption option) {
        return switch (option) {
            case FEATURED -> featuredBaseSort();
            case PRICE_ASC -> Sort.by(Sort.Order.desc("inStock"), Sort.Order.asc("minPrice"), Sort.Order.desc("id"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("inStock"), Sort.Order.desc("minPrice"), Sort.Order.desc("id"));
            case DISCOUNT -> Sort.by(Sort.Order.desc("inStock"), Sort.Order.desc("discountPercent"), Sort.Order.asc("minPrice"), Sort.Order.desc("id"));
            case NEWEST -> Sort.by(Sort.Order.desc("inStock"), Sort.Order.desc("sourceCreatedAt"), Sort.Order.desc("id"));
        };
    }

    private Sort featuredBaseSort() {
        return Sort.by(Sort.Order.desc("inStock"), Sort.Order.desc("sourceCreatedAt"), Sort.Order.desc("id"));
    }

    private List<Product> featuredOrder(List<Product> products, KitQuery query) {
        List<List<Product>> tiers = java.util.stream.Stream.generate(ArrayList<Product>::new)
                .limit(4)
                .map(list -> (List<Product>) list)
                .toList();
        for (Product product : products) {
            tiers.get(featuredTier(product)).add(product);
        }

        long seed = LocalDate.now(clock.withZone(ZoneOffset.UTC)).toEpochDay() * 31L + featuredQuerySeed(query);
        List<Product> ordered = new ArrayList<>(products.size());
        for (int tier = 0; tier < tiers.size(); tier++) {
            ordered.addAll(interleaveStores(tiers.get(tier), seed + tier));
        }
        return List.copyOf(ordered);
    }

    private int featuredTier(Product product) {
        boolean hasImage = product.getImageUrl() != null && !product.getImageUrl().isBlank();
        if (product.isInStock()) {
            return hasImage ? 0 : 1;
        }
        return hasImage ? 2 : 3;
    }

    private List<Product> interleaveStores(List<Product> products, long seed) {
        Map<String, List<Product>> byStore = new LinkedHashMap<>();
        for (Product product : products) {
            byStore.computeIfAbsent(product.getStore().getSlug(), unused -> new ArrayList<>()).add(product);
        }

        List<Map.Entry<String, List<Product>>> stores = new ArrayList<>(byStore.entrySet());
        Collections.shuffle(stores, new Random(seed));
        for (Map.Entry<String, List<Product>> store : stores) {
            Collections.shuffle(store.getValue(), new Random(seed ^ store.getKey().hashCode()));
        }

        List<Product> ordered = new ArrayList<>(products.size());
        boolean added;
        do {
            added = false;
            for (Map.Entry<String, List<Product>> store : stores) {
                if (!store.getValue().isEmpty()) {
                    ordered.add(store.getValue().removeFirst());
                    added = true;
                }
            }
        } while (added);
        return ordered;
    }

    private long featuredQuerySeed(KitQuery query) {
        return java.util.Objects.hash(
                query.ids(), query.text(), query.stores(), query.sizes(), query.versions(), query.inStockOnly(),
                query.onClearance(), query.minPrice(), query.maxPrice(), query.includeOtherKinds());
    }

    private int totalPages(long totalElements, int pageSize) {
        return (int) Math.ceil((double) totalElements / pageSize);
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not exceed maxPrice");
        }
    }
}
