package com.kitradar.ingest.dotpe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitradar.ingest.model.RawProduct;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DotPeMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DotPeMapper mapper = new DotPeMapper();

    @Test
    void mapsTheDiscountedVariantPriceAndMerchantCategory() throws IOException {
        DotPeProductsResponse response = objectMapper.readValue(
                new ClassPathResource("fixtures/dotpe/product.json").getInputStream(), DotPeProductsResponse.class);

        RawProduct product = mapper.map(response.pageProps().product(), "https://www.goalthread.co.in");

        assertThat(product.externalId()).isEqualTo("33190335");
        assertThat(product.category()).isEqualTo("FAN VERSION");
        assertThat(product.tags()).containsExactly("Spain");
        assertThat(product.variants()).singleElement().satisfies(variant -> {
            assertThat(variant.sizeLabel()).isEqualTo("S");
            assertThat(variant.price()).isEqualByComparingTo("949");
            assertThat(variant.compareAtPrice()).isEqualByComparingTo("1249");
            assertThat(variant.available()).isTrue();
        });
    }
}
