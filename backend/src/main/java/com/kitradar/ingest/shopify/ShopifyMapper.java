package com.kitradar.ingest.shopify;

import com.kitradar.ingest.model.RawProduct;
import com.kitradar.ingest.model.RawVariant;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ShopifyMapper {

    public RawProduct map(ShopifyProductJson product, String baseUrl) {
        int sizeOptionPosition = sizeOptionPosition(product.options());
        List<RawVariant> variants = safe(product.variants()).stream()
                .map(variant -> mapVariant(variant, sizeOptionPosition))
                .toList();

        return new RawProduct(
                Long.toString(product.id()),
                product.title(),
                normalizedBaseUrl(baseUrl) + "/products/" + product.handle(),
                safe(product.images()).stream().map(ShopifyProductJson.ShopifyImageJson::src).findFirst().orElse(null),
                safe(product.tags()),
                null,
                parseInstant(product.createdAt()),
                variants);
    }

    private RawVariant mapVariant(ShopifyProductJson.ShopifyVariantJson variant, int sizeOptionPosition) {
        return new RawVariant(
                Long.toString(variant.id()),
                sizeAt(variant, sizeOptionPosition),
                new BigDecimal(variant.price()),
                variant.compareAtPrice() == null ? null : new BigDecimal(variant.compareAtPrice()),
                variant.available(),
                variant.position());
    }

    private int sizeOptionPosition(List<ShopifyProductJson.ShopifyOptionJson> options) {
        return safe(options).stream()
                .filter(option -> option.name() != null && option.name().trim().equalsIgnoreCase("size"))
                .mapToInt(ShopifyProductJson.ShopifyOptionJson::position)
                .findFirst()
                .orElse(0);
    }

    private String sizeAt(ShopifyProductJson.ShopifyVariantJson variant, int position) {
        return switch (position) {
            case 1 -> variant.option1();
            case 2 -> variant.option2();
            case 3 -> variant.option3();
            default -> null;
        };
    }

    private Instant parseInstant(String value) {
        return value == null || value.isBlank() ? null : OffsetDateTime.parse(value).toInstant();
    }

    private String normalizedBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private <T> List<T> safe(List<T> items) {
        return items == null ? List.of() : items;
    }
}
