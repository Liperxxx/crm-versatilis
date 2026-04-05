package com.versatilis.crm.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class EnvironmentConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String hibernateDdlAuto;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.environment:development}")
    private String environment;

    @Value("${app.debug:false}")
    private boolean debug;

    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }

    public boolean isDevelopment() {
        return "development".equalsIgnoreCase(environment);
    }
}