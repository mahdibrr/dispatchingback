Render Cold-Start & Lazy-Start Mitigations

Summary
- Render Free web services spin down after ~15 minutes with no traffic. Cold-start (spin-up) can take up to ~1 minute.
- The only way to avoid spin-down on Render Free is to use an external scheduler that periodically pings a lightweight endpoint.

Quick Mitigations (no Upgrade Required)
- External warm-up pings: Use an external scheduler (e.g., cron-job.org, UptimeRobot, GitHub Actions, or an external VM) to hit a minimal endpoint every 10–14 minutes to prevent spin-down.
- Keep the warm-up endpoint minimal: return HTTP 204, no DB or template work.

Spring Boot Startup Hardening
- Avoid schema generation and heavy Hibernate initialization in production:
  - `spring.jpa.hibernate.ddl-auto=none`
  - Use Flyway or Liquibase for migrations run outside of runtime boot.
- Trim auto-configuration: add only what you need to `spring.factories` / `spring.autoconfigure.exclude` to skip unused starters (JDBC, JPA, WebFlux, unused Actuator parts).
- Disable banner and consider properties format if measured faster:
  - `spring.main.banner-mode=off`
- Reduce logging work:
  - `logging.level.root=INFO`

JVM Flags to Improve Time-To-First-Request
- Example environment variable (set in Render web service settings or container env):
  - `JAVA_TOOL_OPTIONS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC"`
- Consider Class Data Sharing (CDS/AppCDS) to reduce class-loading cost:
  - Build shared archive during image build and run with `-Xshare:on`.

AOT / Native Image (Best Cold-Start Improvement)
- Use Spring Boot 3 + native image (GraalVM) to drop cold start times dramatically.
- Maven example: `./mvnw -Pnative spring-boot:build-image`
- Gradle example: `./gradlew bootBuildImage --imageName=app:native`
- Native images reduce startup to sub-seconds–few seconds depending on app complexity.

Container & Docker Tips
- Use a multi-stage Dockerfile and final stage based on a slim JRE or native image base (distroless/alpine) to reduce image size and IO at boot.
- Avoid running DB migrations on every boot; run migrations in a separate one-off job in deployment pipeline.
- Keep your health-check endpoint fast and in-memory (e.g., `/healthz`) and point Render health checks at it.

Render Health & Actuator
- If using Spring Boot Actuator and probes, enable probes and set base path:
  - `management.endpoint.health.probes.enabled=true`
  - `management.endpoints.web.base-path=/actuator`
- Point Render's health check to `/actuator/health/liveness` or your fast health endpoint.

If You Must Stay on Free Tier
- Use external warm-ups + minimize startup work (JVM flags, trim autoconfig, disable banner, lightweight health endpoint). Free tier spin-down cannot be disabled on Render.

Examples
- Minimal warm-up controller (Spring Boot):

```java
@RestController
public class WarmupController {
    @GetMapping(path = "/warmup")
    public ResponseEntity<Void> warmup() {
        return ResponseEntity.noContent().build();
    }
}
```

- Fast health endpoint (no DB):

```java
@RestController
public class HealthzController {
    @GetMapping(path = "/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("ok");
    }
}
```

Follow-ups
- I can add a minimal warm-up endpoint to this project and a sample GitHub Actions workflow to call it every 10 minutes. Would you like that?
- I can also add example `application.properties` settings or a sample multi-stage `Dockerfile` if you want.
