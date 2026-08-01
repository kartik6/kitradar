package com.kitradar.normalize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PriceCalculator {

    public Pricing calculate(List<PriceVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("A product requires at least one variant");
        }

        List<PriceVariant> candidates = variants.stream().anyMatch(PriceVariant::available)
                ? variants.stream().filter(PriceVariant::available).toList()
                : variants;

        PriceVariant displayVariant = candidates.stream()
                .min(Comparator.comparing(PriceVariant::price).thenComparingInt(PriceVariant::position))
                .orElseThrow();

        BigDecimal sellingPrice = displayVariant.price();
        BigDecimal referencePrice = displayVariant.compareAtPrice();
        if (referencePrice == null || referencePrice.compareTo(sellingPrice) <= 0) {
            return new Pricing(sellingPrice, null, 0);
        }

        int discountPercent = referencePrice.subtract(sellingPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(referencePrice, 0, RoundingMode.HALF_UP)
                .intValueExact();
        return new Pricing(sellingPrice, referencePrice, discountPercent);
    }

    public record PriceVariant(BigDecimal price, BigDecimal compareAtPrice, boolean available, int position) {
        public PriceVariant {
            if (price == null || price.signum() < 0) {
                throw new IllegalArgumentException("Variant price must be zero or greater");
            }
        }
    }

    public record Pricing(BigDecimal sellingPrice, BigDecimal compareAtPrice, int discountPercent) {
    }
}
