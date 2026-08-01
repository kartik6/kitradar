package com.kitradar.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import com.kitradar.domain.ProductKind;
import org.junit.jupiter.api.Test;

class ProductKindClassifierTest {

    private final ProductKindClassifier classifier = new ProductKindClassifier();

    @Test
    void excludesKnownNonJerseyListings() {
        assertThat(classifier.classify("Barcelona Home Shorts")).isEqualTo(ProductKind.OTHER);
        assertThat(classifier.classify("Real Madrid Match Ball")).isEqualTo(ProductKind.OTHER);
    }

    @Test
    void keepsJerseyListings() {
        assertThat(classifier.classify("Real Madrid Home Fan Version 25/26")).isEqualTo(ProductKind.JERSEY);
    }
}
