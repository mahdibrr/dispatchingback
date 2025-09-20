package org.example.dispatcher.controller;

import org.example.shared.realtime.CentrifugoTokenService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/realtime", produces = MediaType.APPLICATION_JSON_VALUE)
public class RealtimeController {

    private final CentrifugoTokenService centrifugoTokenService;

    public RealtimeController(CentrifugoTokenService centrifugoTokenService) {
        this.centrifugoTokenService = centrifugoTokenService;
    }

    public static record MissionTokenRequest(UUID missionId) {}

    @PostMapping(path = "/mission-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> issueMissionToken(
            @RequestBody MissionTokenRequest req,
            Authentication auth
    ) {
        if (req == null || req.missionId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missionId required"));
        }
        String channel = "missions:" + req.missionId();
        String userId = (auth != null ? auth.getName() : "anonymous");
        String token = centrifugoTokenService.issueSubscriptionToken(userId, channel);
        return ResponseEntity.ok(Map.of("subscriptionToken", token, "channel", channel));
    }

    public static record DriverTokenRequest(UUID driverId) {}

    @PostMapping(path = "/driver-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> issueDriverToken(
            @RequestBody DriverTokenRequest req,
            Authentication auth
    ) {
        if (req == null || req.driverId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "driverId required"));
        }
        String channel = "drivers:" + req.driverId();
        String userId = (auth != null ? auth.getName() : "anonymous");
        String token = centrifugoTokenService.issueSubscriptionToken(userId, channel);
        return ResponseEntity.ok(Map.of("subscriptionToken", token, "channel", channel));
    }
}
