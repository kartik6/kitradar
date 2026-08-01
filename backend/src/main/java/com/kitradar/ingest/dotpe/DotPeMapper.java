package com.kitradar.ingest.dotpe;

import com.kitradar.ingest.model.RawProduct;
import com.kitradar.ingest.model.RawVariant;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DotPeMapper {

    public RawProduct map(DotPeProductJson product, String baseUrl) {
        List<RawVariant> variants = safe(product.variants()).stream().map(this::mapVariant).toList();
        if (variants.isEmpty()) {
            variants = List.of(new RawVariant(
                    Long.toString(product.id()),
                    null,
                    sellingPrice(product.price(), product.discountedPrice()),
                    referencePrice(product.price(), product.discountedPrice()),
                    false,
                    0));
        }
        return new RawProduct(
                Long.toString(product.id()),
                product.name(),
                product.link() == null || product.link().isBlank() ? baseUrl + "/product/" + product.id() : product.link(),
                product.imageUrl(),
                safe(product.collections()).stream().map(DotPeProductJson.DotPeCollectionJson::name).filter(name -> name != null).toList(),
                product.category() == null ? null : product.category().name(),
                null,
                variants);
    }

    private RawVariant mapVariant(DotPeProductJson.DotPeVariantJson variant) {
        return new RawVariant(
                Long.toString(variant.variantId()),
                sizeLabel(variant),
                sellingPrice(variant.price(), variant.discountedPrice()),
                referencePrice(variant.price(), variant.discountedPrice()),
                variant.available() == 1,
                0);
    }

    private String sizeLabel(DotPeProductJson.DotPeVariantJson variant) {
        return safe(variant.optionVariantNames()).stream()
                .filter(option -> option.optionName() != null && option.optionName().equalsIgnoreCase("Size"))
                .map(DotPeProductJson.DotPeOptionValueJson::variantName)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal sellingPrice(BigDecimal price, BigDecimal discountedPrice) {
        return discountedPrice == null ? price : discountedPrice;
    }

    private BigDecimal referencePrice(BigDecimal price, BigDecimal discountedPrice) {
        return discountedPrice != null && price != null && price.compareTo(discountedPrice) > 0 ? price : null;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
