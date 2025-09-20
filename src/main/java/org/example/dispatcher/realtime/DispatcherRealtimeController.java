package org.example.dispatcher.realtime;

import org.example.shared.realtime.CentrifugoTokenService;
import org.example.shared.repository.UserRepository;
import org.example.shared.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/realtime")
public class DispatcherRealtimeController {

    @Autowired
    private CentrifugoTokenService centrifugoTokenService;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/connection-token")
    public ResponseEntity<Map<String, String>> getConnectionToken(Authentication authentication) {
        try {
            String userId = authentication != null ? authentication.getName() : "anonymous";
            String token = centrifugoTokenService.issueConnectionToken(userId, null);
            return ResponseEntity.ok(Map.of("connectionToken", token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/mission-assignments-token")
    public ResponseEntity<Map<String, String>> getMissionAssignmentsToken(Authentication authentication) {
        try {
            String userId = authentication.getName();
            // Dispatchers listen to general mission assignment notifications
            String token = centrifugoTokenService.issueSubscriptionToken(userId, "missions");
            return ResponseEntity.ok(Map.of("subscriptionToken", token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/mission-status-token")
    public ResponseEntity<Map<String, String>> getMissionStatusToken(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        try {
            String userId = authentication.getName();
            String missionId = (String) body.get("missionId");
            
            if (missionId == null || missionId.trim().isEmpty()) {
                // General mission status updates for dispatchers
                String token = centrifugoTokenService.issueSubscriptionToken(userId, "status");
                return ResponseEntity.ok(Map.of("subscriptionToken", token));
            }
            
            // Specific mission status updates (same channel as location updates)
            String channel = "missions:" + missionId.trim();
            String token = centrifugoTokenService.issueSubscriptionToken(userId, channel);
            return ResponseEntity.ok(Map.of("subscriptionToken", token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}