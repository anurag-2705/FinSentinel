package com.finsentinel.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .cors(cors ->
                cors.configurationSource(corsConfigurationSource())
            )

            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/verify-email",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/actuator/health"
                ).permitAll()

                // Admin User Management
                .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")

                // Accounts
                .requestMatchers("/api/v1/accounts/**")
                    .authenticated()

                // Transactions
                .requestMatchers("/api/v1/transactions/**")
                    .authenticated()

                // Beneficiaries
                .requestMatchers("/api/v1/beneficiaries/**")
                    .authenticated()

                // Fraud Management
                .requestMatchers("/api/v1/fraud/**")
                    .hasAnyRole("FRAUD_ANALYST", "ADMIN")

                // ML Service
                .requestMatchers("/api/v1/ml/**")
                    .hasAnyRole("FRAUD_ANALYST", "ADMIN")

                .anyRequest().denyAll()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                org.springframework.security.web.authentication
                    .UsernamePasswordAuthenticationFilter.class
            )

            .formLogin(form -> form.disable())

            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "Idempotency-Key"
            )
        );

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}