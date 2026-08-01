package com.kitradar.api.mapper;

import com.kitradar.api.dto.KitCardDto;
import com.kitradar.api.dto.KitSizeDto;
import com.kitradar.domain.Product;
import com.kitradar.domain.SizeNorm;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KitDtoMapper {

    public KitCardDto toCard(Product product) {
        Map<SizeNorm, Boolean> sizes = new LinkedHashMap<>();
        product.getVariants().stream()
                .filter(variant -> variant.getSizeNorm() != null)
                .forEach(variant -> sizes.merge(variant.getSizeNorm(), variant.isAvailable(), Boolean::logicalOr));
        List<KitSizeDto> sizeDtos = sizes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new KitSizeDto(entry.getKey().name(), entry.getValue()))
                .toList();
        return new KitCardDto(
                product.getId(),
                product.getTitle(),
                product.getStore().getSlug(),
                product.getStore().getName(),
                product.getImageUrl(),
                product.getProductUrl(),
                product.getVersion(),
                product.getMinPrice(),
                product.getCompareAtPrice(),
                product.getDiscountPercent(),
                product.isInStock(),
                sizeDtos);
    }
}
