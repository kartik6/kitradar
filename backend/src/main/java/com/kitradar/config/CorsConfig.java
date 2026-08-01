package com.kitradar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the separately deployed Next.js application to read the public catalogue API.
 * Administrative endpoints deliberately remain outside this mapping.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String frontendOrigin;

    public CorsConfig(@Value("${kitradar.frontend-origin:http://localhost:3000}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/kits/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET")
                .maxAge(3600);
        registry.addMapping("/api/v1/stores/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET")
                .maxAge(3600);
        registry.addMapping("/api/v1/meta/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET")
                .maxAge(3600);
    }
}
