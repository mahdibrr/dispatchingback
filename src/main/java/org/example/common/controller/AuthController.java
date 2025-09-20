package org.example.common.controller;

import jakarta.validation.Valid;
import org.example.common.dto.LoginRequest;
import org.example.common.dto.SignupRequest;
import org.example.common.dto.UserDto;
import org.example.shared.entity.User;
import org.example.shared.entity.UserRole;
import org.example.shared.repository.UserRepository;
import org.example.shared.realtime.CentrifugoApi;
import org.example.shared.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CentrifugoApi centrifugoApi;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          CentrifugoApi centrifugoApi) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.centrifugoApi = centrifugoApi;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email_exists"));
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone() == null ? null : req.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));

        try {
            if (req.role() != null) {
                String r = req.role().trim().toUpperCase();
                if ("DRIVER".equals(r)) user.setRole(UserRole.DRIVER);
                else if ("DISPATCHER".equals(r)) user.setRole(UserRole.DISPATCHER);
            }
        } catch (Exception ignored) {}

        userRepository.save(user);

        UserDto userDto = new UserDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole().name()
        );

        return ResponseEntity.ok(Map.of("user", userDto));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty() || !passwordEncoder.matches(req.password(), opt.get().getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        User user = opt.get();
        String accessToken = jwtService.issueAccessToken(user);

        UserDto userDto = new UserDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole().name()
        );

        // Do not issue Centrifugo tokens here. Use /realtime endpoints after login.
        return ResponseEntity.ok(Map.of(
            "user", userDto,
            "accessToken", accessToken
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "user") User user) {
        try {
            centrifugoApi.publish("missions", Map.of(
                "type", "logout",
                "userId", user.getId(),
                "at", Instant.now().toString()
            ));
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
