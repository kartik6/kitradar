package com.kitradar.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductVariantSyncTest {

    @Test
    void updatesAnExistingVariantInsteadOfReplacingItWithTheSameExternalId() {
        Product product = new Product(
                new Store("test-store", "TEST STORE", "https://example.test", Platform.SHOPIFY),
                productData(variant("variant-1", "M", "999.00", true)),
                Instant.parse("2026-08-01T00:00:00Z"));
        ProductVariant original = product.getVariants().getFirst();

        product.apply(
                productData(variant("variant-1", "L", "899.00", false)),
                Instant.parse("2026-08-01T01:00:00Z"));

        assertThat(product.getVariants()).containsExactly(original);
        assertThat(original.getSizeLabel()).isEqualTo("L");
        assertThat(original.getPrice()).isEqualByComparingTo("899.00");
        assertThat(original.isAvailable()).isFalse();
    }

    @Test
    void keepsOnlyOneVariantWhenAnUpstreamPayloadRepeatsAnExternalId() {
        Product product = new Product(
                new Store("test-store", "TEST STORE", "https://example.test", Platform.SHOPIFY),
                productData(variant("variant-1", "M", "999.00", true)),
                Instant.parse("2026-08-01T00:00:00Z"));

        product.apply(
                productData(
                        variant("variant-1", "M", "999.00", true),
                        variant("variant-1", "L", "899.00", false)),
                Instant.parse("2026-08-01T01:00:00Z"));

        assertThat(product.getVariants()).hasSize(1);
    }

    private ProductData productData(ProductData.VariantData... variants) {
        return new ProductData(
                "product-1",
                "Test jersey",
                "test jersey",
                "https://example.test/product-1",
                null,
                KitVersion.FAN,
                ProductKind.JERSEY,
                new BigDecimal("899.00"),
                null,
                0,
                true,
                null,
                List.of(variants));
    }

    private ProductData.VariantData variant(String externalId, String size, String price, boolean available) {
        return new ProductData.VariantData(
                externalId,
                size,
                SizeNorm.valueOf(size),
                new BigDecimal(price),
                null,
                available,
                0);
    }
}
