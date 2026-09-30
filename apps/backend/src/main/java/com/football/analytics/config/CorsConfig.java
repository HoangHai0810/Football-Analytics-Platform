package com.football.analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Allows configuring CORS origins via environment variable CORS_ALLOWED_ORIGINS
    // Default: permits localhost dev origins + any *.vercel.app domain
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Build patterns: always include localhost origins + the configured list + Vercel wildcard
        String[] patterns = buildPatterns();
        registry.addMapping("/**")
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    private String[] buildPatterns() {
        // Always allow Vercel preview + production deployments
        List<String> all = new java.util.ArrayList<>(allowedOrigins);
        all.add("https://*.vercel.app");
        all.add("https://*.onrender.com");
        return all.toArray(new String[0]);
    }
}
