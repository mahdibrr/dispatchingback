package org.example.shared.realtime;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class CentrifugoTokenService {

    private static final Logger log = LoggerFactory.getLogger(CentrifugoTokenService.class);

    @Value("${app.centrifugo.token-hmac-secret-key:dev_hmac_fallback_1234567890abcdef}")
    private String hmacSecret;

    @Value("${app.centrifugo.token-ttl-seconds:3600}")
    private long ttlSeconds;

    @Value("${app.centrifugo.private-key-path:}")
    private String privateKeyPath;

    private Algorithm algorithm;
    private String mode;

    @PostConstruct
    public void init() {
        boolean triedRsa = false;

        // Try RSA first if path is provided
        if (privateKeyPath != null && !privateKeyPath.trim().isEmpty()) {
            try {
                triedRsa = true;
                ClassPathResource resource = new ClassPathResource(privateKeyPath.replace("classpath:", ""));
                try (InputStream in = resource.getInputStream()) {
                    byte[] keyBytes = in.readAllBytes();
                    String keyPem = new String(keyBytes)
                            .replace("-----BEGIN PRIVATE KEY-----", "")
                            .replace("-----END PRIVATE KEY-----", "")
                            .replaceAll("\\s", "");
                    byte[] decoded = java.util.Base64.getDecoder().decode(keyPem);
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
                    KeyFactory kf = KeyFactory.getInstance("RSA");

                    // Cast explicitly to RSAPrivateKey
                    RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(spec);

                    this.algorithm = Algorithm.RSA256(null, privateKey);
                    this.mode = "RSA";
                    log.info("Centrifugo token mode=RSA (privateKeyPath={}) triedRsa={}", privateKeyPath, triedRsa);
                    return;
                }
            } catch (Exception e) {
                log.warn("RSA private key not found at {} ({}). Will consider HMAC fallback.", privateKeyPath, e.getMessage());
            }
        }

        // Fallback to HMAC
        if (hmacSecret != null && !hmacSecret.trim().isEmpty()) {
            this.algorithm = Algorithm.HMAC256(hmacSecret);
            this.mode = "HMAC";
            String secretPrefix = hmacSecret.length() > 12 ? hmacSecret.substring(0, 12) + "..." : hmacSecret;
            log.info("Centrifugo token mode=HMAC (secret prefix={}) triedRsa={}", secretPrefix, triedRsa);
        } else {
            throw new IllegalStateException("No RSA private key found and no HMAC secret provided. " +
                    "Provide app.centrifugo.private-key-path or app.centrifugo.token-hmac-secret-key");
        }
    }

    public String issueConnectionToken(String userId, Map<String, Object> info) {
        Instant exp = Instant.now().plusSeconds(ttlSeconds);
        var builder = JWT.create()
                .withSubject(userId)
                .withExpiresAt(Date.from(exp));

        if (info != null && !info.isEmpty()) {
            builder.withClaim("info", info);
        }

        return builder.sign(algorithm);
    }

    public String issueSubscriptionToken(String userId, String channel) {
        Instant exp = Instant.now().plusSeconds(ttlSeconds);
        return JWT.create()
                .withSubject(userId)
                .withExpiresAt(Date.from(exp))
                .withClaim("channel", channel)
                .sign(algorithm);
    }
}
