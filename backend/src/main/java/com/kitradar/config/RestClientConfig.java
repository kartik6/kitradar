package com.kitradar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient storeRestClient(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "KitPagluBot/1.0 (+https://kitpaglu.vercel.app)")
                .messageConverters(converters -> converters.stream()
                        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                        .findFirst()
                        .ifPresent(converter -> ((MappingJackson2HttpMessageConverter) converter)
                                .setObjectMapper(objectMapper.copy()
                                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))))
                .build();
    }
}
