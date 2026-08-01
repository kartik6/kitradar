package com.kitradar.search;

import com.kitradar.api.dto.MetaDto;
import com.kitradar.api.dto.StoreDto;
import com.kitradar.domain.KitVersion;
import com.kitradar.domain.ProductKind;
import com.kitradar.domain.SizeNorm;
import com.kitradar.repository.ProductRepository;
import com.kitradar.repository.StoreRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetaService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    public MetaService(StoreRepository storeRepository, ProductRepository productRepository) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<StoreDto> stores() {
        return storeRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(store -> new StoreDto(
                        store.getSlug(),
                        store.getName(),
                        productRepository.countByStoreIdAndActiveTrueAndProductKind(store.getId(), ProductKind.JERSEY)))
                .filter(store -> store.productCount() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable("meta")
    public MetaDto meta() {
        List<StoreDto> stores = stores();
        Instant lastUpdatedAt = storeRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(store -> store.getLastScrapedAt())
                .filter(value -> value != null)
                .max(Instant::compareTo)
                .orElse(null);
        ProductRepository.PriceRange prices = productRepository.findActivePriceRange(ProductKind.JERSEY);
        return new MetaDto(
                productRepository.countByActiveTrueAndProductKind(ProductKind.JERSEY),
                stores.size(),
                lastUpdatedAt,
                List.of(SizeNorm.S.name(), SizeNorm.M.name(), SizeNorm.L.name(), SizeNorm.XL.name(), SizeNorm.XXL.name(), SizeNorm.XXXL.name()),
                List.of(KitVersion.FAN, KitVersion.PLAYER, KitVersion.MASTER, KitVersion.RETRO,
                        KitVersion.KIDS, KitVersion.WOMEN, KitVersion.LONG_SLEEVE),
                prices.getMinPrice(),
                prices.getMaxPrice());
    }
}
