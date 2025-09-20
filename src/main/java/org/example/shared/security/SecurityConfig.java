// src/main/java/org/example/shared/security/SecurityConfig.java
package org.example.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                .requestMatchers(
                    "/swagger-ui.html", "/swagger-ui/**",
                    "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                    "/favicon.ico", "/webjars/**"
                ).permitAll()
                .requestMatchers("/realtime/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .headers(h -> h
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                // Durcissement optionnel :
                //.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                //.frameOptions(frame -> frame.sameOrigin())
                //.xssProtection(xss -> xss.block(true))
                //.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if ("*".equals(allowedOrigins)) {
            config.addAllowedOriginPattern("*"); // autoriser les credentials avec wildcard
        } else {
            for (String o : allowedOrigins.split(",")) {
                String origin = o.trim();
                if (origin.isEmpty()) continue;
                // use origin patterns so both exact origins and patterns are accepted
                config.addAllowedOriginPattern(origin);
            }
        }
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("ETag"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Ensure CORS headers are applied as early as possible (handles preflight even when other filters run)
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", (CorsConfiguration) corsConfigurationSource().getCorsConfiguration(null));
        return new CorsFilter(source);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
