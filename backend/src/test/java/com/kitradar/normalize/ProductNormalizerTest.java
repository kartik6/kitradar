package com.kitradar.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import com.kitradar.domain.KitVersion;
import com.kitradar.domain.ProductKind;
import com.kitradar.domain.SizeNorm;
import com.kitradar.ingest.model.RawProduct;
import com.kitradar.ingest.model.RawVariant;
import com.kitradar.normalize.version.CategoryVersionRule;
import com.kitradar.normalize.version.DefaultVersionRule;
import com.kitradar.normalize.version.TagVersionRule;
import com.kitradar.normalize.version.TitleVersionRule;
import com.kitradar.normalize.version.VersionResolver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductNormalizerTest {

    private final ProductNormalizer normalizer = new ProductNormalizer(
            new SizeNormalizer(),
            new ProductKindClassifier(),
            new PriceCalculator(),
            new VersionResolver(List.of(
                    new CategoryVersionRule(),
                    new TagVersionRule(),
                    new TitleVersionRule(Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC)),
                    new DefaultVersionRule())));

    @Test
    void normalizesSearchVersionSizesAndDisplayPrice() {
        var normalized = normalizer.normalize(new RawProduct(
                "external-1",
                "Bárcelona Fan Version 25/26",
                "https://store.example/product",
                null,
                List.of("FC Barcelona"),
                null,
                null,
                List.of(new RawVariant("variant-1", "3XL", new BigDecimal("699"), new BigDecimal("999"), true, 1))));

        assertThat(normalized.searchText()).isEqualTo("barcelona fan version 25/26 fc barcelona");
        assertThat(normalized.version()).isEqualTo(KitVersion.FAN);
        assertThat(normalized.productKind()).isEqualTo(ProductKind.JERSEY);
        assertThat(normalized.variants()).singleElement().extracting(variant -> variant.sizeNorm()).isEqualTo(SizeNorm.XXXL);
        assertThat(normalized.minPrice()).isEqualByComparingTo("699");
        assertThat(normalized.compareAtPrice()).isEqualByComparingTo("999");
    }
}
