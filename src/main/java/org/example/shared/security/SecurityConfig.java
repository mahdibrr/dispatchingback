// src/main/java/org/example/shared/security/SecurityConfig.java
package org.example.shared.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:*}") // ENV: APP_CORS_ALLOWED_ORIGINS
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/refresh").permitAll()
                .requestMatchers(
                    "/swagger-ui.html", "/swagger-ui/**",
                    "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                    "/favicon.ico", "/webjars/**", "/actuator/**",
                    "/warmup", "/healthz"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();

        List<String> origins = parseOrigins(allowedOrigins);
        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            c.addAllowedOriginPattern("*"); // dev only; avoid in prod with credentials
        } else {
            origins.forEach(c::addAllowedOrigin);
        }

        c.setAllowCredentials(true);
        c.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(Arrays.asList("Authorization","Content-Type"));
        c.setExposedHeaders(Arrays.asList("ETag","Location","Link"));
        c.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    private List<String> parseOrigins(String csv) {
        String[] parts = csv == null ? new String[0] : csv.split(",");
        List<String> out = new ArrayList<>(parts.length);
        for (String raw : parts) {
            if (raw == null) continue;
            String o = raw.trim();
            if (o.endsWith("/")) o = o.substring(0, o.length() - 1);
            if (!o.isEmpty()) out.add(o);
        }
        if (out.isEmpty()) out.add("*");
        return out;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
