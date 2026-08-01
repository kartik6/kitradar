package com.kitradar.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void usesTheSameCheapestAvailableVariantForSellingPriceAndMrp() {
        PriceCalculator.Pricing pricing = calculator.calculate(List.of(
                variant("699", "999", true, 2),
                variant("599", "1499", true, 1)));

        assertThat(pricing.sellingPrice()).isEqualByComparingTo("599");
        assertThat(pricing.compareAtPrice()).isEqualByComparingTo("1499");
        assertThat(pricing.discountPercent()).isEqualTo(60);
    }

    @Test
    void ignoresAReferencePriceBelongingToAnotherVariant() {
        PriceCalculator.Pricing pricing = calculator.calculate(List.of(
                variant("599", null, true, 1),
                variant("699", "1499", true, 2)));

        assertThat(pricing.sellingPrice()).isEqualByComparingTo("599");
        assertThat(pricing.compareAtPrice()).isNull();
        assertThat(pricing.discountPercent()).isZero();
    }

    @Test
    void prefersAvailableVariantsBeforeUsingTheirPrice() {
        PriceCalculator.Pricing pricing = calculator.calculate(List.of(
                variant("499", "999", false, 1),
                variant("699", "999", true, 2)));

        assertThat(pricing.sellingPrice()).isEqualByComparingTo("699");
        assertThat(pricing.compareAtPrice()).isEqualByComparingTo("999");
    }

    private PriceCalculator.PriceVariant variant(String price, String compareAtPrice, boolean available, int position) {
        return new PriceCalculator.PriceVariant(
                new BigDecimal(price),
                compareAtPrice == null ? null : new BigDecimal(compareAtPrice),
                available,
                position);
    }
}
