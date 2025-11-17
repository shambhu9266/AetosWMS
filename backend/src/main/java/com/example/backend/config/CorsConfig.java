package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed.origins:http://localhost:4200,http://20.57.79.136,http://20.57.79.136:80,http://20.57.79.136:8080}")
    private String allowedOrigins;
    
    // Also allow requests from the same IP without explicit port (defaults to port 80)
    private static final String[] ADDITIONAL_ORIGINS = {
        "http://20.57.79.136",
        "http://20.57.79.136:80"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Split the comma-separated origins from configuration
        String[] origins = allowedOrigins.split(",");
        
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Split the comma-separated origins from configuration
        String[] origins = allowedOrigins.split(",");
        for (String origin : origins) {
            String trimmedOrigin = origin.trim();
            if (!trimmedOrigin.isEmpty()) {
                configuration.addAllowedOrigin(trimmedOrigin);
            }
        }
        
        // Add additional origins explicitly
        for (String origin : ADDITIONAL_ORIGINS) {
            configuration.addAllowedOrigin(origin);
        }
        
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
