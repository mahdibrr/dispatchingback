package org.example.shared.config;

import org.example.shared.entity.User;
import org.example.shared.repository.UserRepository;
import org.example.shared.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.UUID;

@Configuration
public class JwtConfig {

    @Bean
    public JwtService jwtService(
        @Value("${app.jwt.access-secret}") String accessSecret,
        @Value("${app.jwt.refresh-secret}") String refreshSecret,
        UserRepository userRepository
    ) {
        return new JwtService(accessSecret, refreshSecret) {
            @Override
            protected Optional<User> loadUserById(String id) {
                try { 
                    return userRepository.findById(UUID.fromString(id)); 
                } catch (IllegalArgumentException e) { 
                    return Optional.empty(); 
                }
            }
        };
    }
}
