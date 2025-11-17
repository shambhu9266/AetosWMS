package com.example.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/requisitions/**").permitAll()
                .requestMatchers("/api/notifications/**").permitAll()
                .requestMatchers("/api/users/**").permitAll()
                .requestMatchers("/api/departments/**").permitAll()
                .requestMatchers("/api/dashboard/**").permitAll()
                .requestMatchers("/api/budgets/**").permitAll()
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions().disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        return http.build();
    }
}
