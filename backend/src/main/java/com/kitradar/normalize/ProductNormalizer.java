package com.kitradar.normalize;

import com.kitradar.domain.ProductData;
import com.kitradar.domain.SizeNorm;
import com.kitradar.ingest.model.RawProduct;
import com.kitradar.ingest.model.RawVariant;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.kitradar.normalize.version.VersionCandidate;
import com.kitradar.normalize.version.VersionResolver;

@Component
public class ProductNormalizer {

    private final SizeNormalizer sizeNormalizer;
    private final ProductKindClassifier productKindClassifier;
    private final PriceCalculator priceCalculator;
    private final VersionResolver versionResolver;

    public ProductNormalizer(
            SizeNormalizer sizeNormalizer,
            ProductKindClassifier productKindClassifier,
            PriceCalculator priceCalculator,
            VersionResolver versionResolver) {
        this.sizeNormalizer = sizeNormalizer;
        this.productKindClassifier = productKindClassifier;
        this.priceCalculator = priceCalculator;
        this.versionResolver = versionResolver;
    }

    public ProductData normalize(RawProduct product) {
        Map<String, ProductData.VariantData> variantsByExternalId = new LinkedHashMap<>();
        for (RawVariant raw : product.variants()) {
            variantsByExternalId.putIfAbsent(raw.externalId(), new ProductData.VariantData(
                    raw.externalId(),
                    raw.sizeLabel(),
                    sizeNormalizer.normalize(raw.sizeLabel()).orElse(null),
                    raw.price(),
                    raw.compareAtPrice(),
                    raw.available(),
                    raw.position()));
        }
        var variants = variantsByExternalId.values().stream().toList();
        var pricing = priceCalculator.calculate(variants.stream()
                .map(variant -> new PriceCalculator.PriceVariant(
                        variant.price(), variant.compareAtPrice(), variant.available(), variant.position()))
                .toList());

        return new ProductData(
                product.externalId(),
                product.title(),
                searchText(product),
                product.productUrl(),
                product.imageUrl(),
                versionResolver.resolve(new VersionCandidate(product.title(), product.tags(), product.category())),
                productKindClassifier.classify(product.title()),
                pricing.sellingPrice(),
                pricing.compareAtPrice(),
                pricing.discountPercent(),
                product.variants().stream().anyMatch(raw -> raw.available()),
                product.sourceCreatedAt(),
                variants);
    }

    private String searchText(RawProduct product) {
        String searchable = String.join(" ", product.title(), String.join(" ", product.tags()));
        return Normalizer.normalize(searchable, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
