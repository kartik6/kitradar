package com.kitradar.search;

import com.kitradar.domain.Product;
import com.kitradar.domain.ProductKind;
import com.kitradar.domain.ProductVariant;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class KitSpecifications {

    private KitSpecifications() {
    }

    public static Specification<Product> build(KitQuery kitQuery) {
        return (root, criteriaQuery, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isTrue(root.get("active")));
            if (!kitQuery.includeOtherKinds()) {
                predicates.add(builder.equal(root.get("productKind"), ProductKind.JERSEY));
            }
            if (!kitQuery.ids().isEmpty()) {
                predicates.add(root.get("id").in(kitQuery.ids()));
            }
            if (!kitQuery.stores().isEmpty()) {
                predicates.add(root.join("store").get("slug").in(kitQuery.stores()));
            }
            if (!kitQuery.versions().isEmpty()) {
                predicates.add(root.get("version").in(kitQuery.versions()));
            }
            if (kitQuery.minPrice() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("minPrice"), kitQuery.minPrice()));
            }
            if (kitQuery.maxPrice() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("minPrice"), kitQuery.maxPrice()));
            }
            if (kitQuery.onClearance()) {
                predicates.add(builder.greaterThan(root.get("discountPercent"), 0));
            }
            addTextPredicates(kitQuery.text(), root, builder, predicates);
            addVariantPredicates(kitQuery, root, criteriaQuery, builder, predicates);
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static void addTextPredicates(
            String text,
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            List<jakarta.persistence.criteria.Predicate> predicates) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String token : normalize(text).split("\\s+")) {
            if (!token.isBlank()) {
                predicates.add(builder.like(root.get("searchText"), "%" + token + "%"));
            }
        }
    }

    private static void addVariantPredicates(
            KitQuery kitQuery,
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaQuery<?> criteriaQuery,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            List<jakarta.persistence.criteria.Predicate> predicates) {
        if (kitQuery.sizes().isEmpty()) {
            if (kitQuery.inStockOnly()) {
                predicates.add(builder.isTrue(root.get("inStock")));
            }
            return;
        }

        jakarta.persistence.criteria.Subquery<Integer> variantQuery = criteriaQuery.subquery(Integer.class);
        jakarta.persistence.criteria.Root<ProductVariant> variant = variantQuery.from(ProductVariant.class);
        List<jakarta.persistence.criteria.Predicate> variantPredicates = new ArrayList<>();
        variantPredicates.add(builder.equal(variant.get("product"), root));
        variantPredicates.add(variant.get("sizeNorm").in(kitQuery.sizes()));
        if (kitQuery.inStockOnly()) {
            variantPredicates.add(builder.isTrue(variant.get("available")));
        }
        variantQuery.select(builder.literal(1));
        variantQuery.where(builder.and(variantPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
        predicates.add(builder.exists(variantQuery));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
