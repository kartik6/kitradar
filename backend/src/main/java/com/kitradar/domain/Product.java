package com.kitradar.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(name = "search_text", nullable = false)
    private String searchText;

    @Column(name = "product_url", nullable = false)
    private String productUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KitVersion version = KitVersion.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_kind", nullable = false)
    private ProductKind productKind = ProductKind.JERSEY;

    @Column(name = "min_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock;

    @Column(name = "source_created_at")
    private Instant sourceCreatedAt;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductVariant> variants = new ArrayList<>();

    public Product(Store store, ProductData data, Instant seenAt) {
        this.store = store;
        this.externalId = data.externalId();
        this.firstSeenAt = seenAt;
        apply(data, seenAt);
    }

    public void apply(ProductData data, Instant seenAt) {
        this.title = data.title();
        this.searchText = data.searchText();
        this.productUrl = data.productUrl();
        this.imageUrl = data.imageUrl();
        this.version = data.version();
        this.productKind = data.productKind();
        this.minPrice = data.minPrice();
        this.compareAtPrice = data.compareAtPrice();
        this.discountPercent = data.discountPercent();
        this.inStock = data.inStock();
        this.sourceCreatedAt = data.sourceCreatedAt();
        this.lastSeenAt = seenAt;
        this.updatedAt = seenAt;
        this.active = true;
        replaceVariants(data.variants().stream().map(ProductVariant::new).toList());
    }

    public void replaceVariants(List<ProductVariant> replacements) {
        variants.clear();
        replacements.forEach(this::addVariant);
    }

    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        variants.add(variant);
    }
}
