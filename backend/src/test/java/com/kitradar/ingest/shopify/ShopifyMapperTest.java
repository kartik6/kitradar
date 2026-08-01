package com.kitradar.ingest.shopify;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitradar.ingest.model.RawProduct;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ShopifyMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ShopifyMapper mapper = new ShopifyMapper();

    @Test
    void mapsStructuredShopifyDataAndUsesTheActualSizeOption() throws IOException {
        ShopifyProductsResponse response = objectMapper.readValue(
                new ClassPathResource("fixtures/shopify/products.json").getInputStream(),
                ShopifyProductsResponse.class);

        RawProduct product = mapper.map(response.products().getFirst(), "https://fulltimestore.in/");

        assertThat(product.externalId()).isEqualTo("9680877945134");
        assertThat(product.productUrl()).isEqualTo("https://fulltimestore.in/products/manchester-united-jersey");
        assertThat(product.imageUrl()).isEqualTo("https://cdn.shopify.com/image.jpg");
        assertThat(product.variants()).singleElement().satisfies(variant -> {
            assertThat(variant.externalId()).isEqualTo("49673686286638");
            assertThat(variant.sizeLabel()).isEqualTo("3XL");
            assertThat(variant.price()).isEqualByComparingTo("599.00");
        });
    }

    @Test
    void leavesSizeNullWhenTheProductHasNoSizeOption() throws IOException {
        ShopifyProductsResponse response = objectMapper.readValue(
                new ClassPathResource("fixtures/shopify/products.json").getInputStream(),
                ShopifyProductsResponse.class);

        RawProduct product = mapper.map(response.products().get(1), "https://fulltimestore.in");

        assertThat(product.imageUrl()).isNull();
        assertThat(product.variants()).singleElement().extracting(variant -> variant.sizeLabel()).isNull();
    }
}
