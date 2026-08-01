package com.kitradar.domain;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_variant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "size_label")
    private String sizeLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "size_norm")
    private SizeNorm sizeNorm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false)
    private int position;

    ProductVariant(ProductData.VariantData data) {
        this.externalId = data.externalId();
        this.sizeLabel = data.sizeLabel();
        this.sizeNorm = data.sizeNorm();
        this.price = data.price();
        this.compareAtPrice = data.compareAtPrice();
        this.available = data.available();
        this.position = data.position();
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
