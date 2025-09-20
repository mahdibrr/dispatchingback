package org.example.shared.security;

import org.example.shared.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class JwtService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    private static final long ACCESS_TTL_SEC  = 9000;                  // 15 min
    private static final long REFRESH_TTL_SEC = 60L * 60L * 24L * 30L; // 30 days
    private static final String ISS = "https://api.yourapp.local";
    private static final String AUD = "yourapp-spa";

    public JwtService(String accessSecret, String refreshSecret) {
        this.accessKey  = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public long accessTokenTtlSeconds() { return ACCESS_TTL_SEC; }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .header().keyId("access-2025-09").and()
            .issuer(ISS)
            .audience().add(AUD).and()
            .subject(user.getId().toString())
            .issuedAt(Date.from(now))
            .notBefore(Date.from(now.minusSeconds(5)))
            .expiration(Date.from(now.plusSeconds(ACCESS_TTL_SEC)))
            .id(UUID.randomUUID().toString()) // jti
            .claim("role", user.getRole().name())
            .claim("scope", "missions:read drivers:read assignments:write")
            .signWith(accessKey) // alg inferred
            .compact();
    }

    public String issueRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .header().keyId("refresh-2025-09").and()
            .issuer(ISS)
            .audience().add("yourapp-refresh").and()
            .subject(user.getId().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(REFRESH_TTL_SEC)))
            .id(UUID.randomUUID().toString()) // jti
            .signWith(refreshKey)
            .compact();
    }

    // ---- Parsing helpers (0.12.x) ----
    public Jws<Claims> parseAccessClaims(String token) {
        return Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token);
    }

    public Jws<Claims> parseRefreshClaims(String token) {
        return Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token);
    }

    public Optional<String> getSubjectIfValidAccess(String token) {
        try {
            return Optional.of(parseAccessClaims(token).getPayload().getSubject());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public Optional<User> verifyAndLoadRefreshToken(String token) {
        try {
            String userId = parseRefreshClaims(token).getPayload().getSubject();
            return loadUserById(userId);
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public String rotateRefreshToken(String oldToken, User user) {
        // mark old jti revoked in store; omitted here
        return issueRefreshToken(user);
    }

    // Wire to repository in concrete service or subclass.
    protected Optional<User> loadUserById(String id) {
        return Optional.empty();
    }
}

