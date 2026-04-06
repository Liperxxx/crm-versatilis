package com.versatilis.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5500}")
    private String allowedOriginsRaw;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Split the comma-separated list from application properties
        String[] configuredOrigins = allowedOriginsRaw.split(",");

        // Build the full list: configured origins + local dev patterns
        String[] localPatterns = {
            "http://localhost:*",
            "http://127.0.0.1:*",
            "null"
        };

        String[] allPatterns = new String[configuredOrigins.length + localPatterns.length];
        for (int i = 0; i < configuredOrigins.length; i++) {
            allPatterns[i] = configuredOrigins[i].trim();
        }
        for (int i = 0; i < localPatterns.length; i++) {
            allPatterns[configuredOrigins.length + i] = localPatterns[i];
        }

        registry.addMapping("/**")
            .allowedOriginPatterns(allPatterns)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}