package org.example.driver.realtime;

import org.example.shared.realtime.CentrifugoTokenService;
import org.example.shared.repository.UserRepository;
import org.example.shared.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/driver/realtime")
public class DriverRealtimeController {

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

    @PostMapping("/mission-token")
    public ResponseEntity<Map<String, String>> getDriverMissionToken(Authentication authentication) {
        try {
            // Get the current user's email from authentication
            String userEmail = authentication.getName();
            
            // Look up the user by email to get their UUID
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
            
            // Create token for driver's UUID-based mission channel
            String channel = "missions:" + user.getId();
            String token = centrifugoTokenService.issueSubscriptionToken(userEmail, channel);
            return ResponseEntity.ok(Map.of("subscriptionToken", token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/location-token")
    public ResponseEntity<Map<String, String>> getLocationToken(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            
            // Look up the user by email to get their UUID
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
            
            // Create token for driver's location updates
            String channel = "driver-location:" + user.getId();
            String token = centrifugoTokenService.issueSubscriptionToken(userEmail, channel);
            return ResponseEntity.ok(Map.of("subscriptionToken", token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/status-token")
    public ResponseEntity<Map<String, String>> getStatusToken(Authentication authentication) {
        try {
            String userId = authentication.getName();
            // Generate token for status channel subscription - allows drivers to publish status updates
            String token = centrifugoTokenService.issueSubscriptionToken(userId, "status");
            return ResponseEntity.ok(Map.of("subscriptionToken", token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/specific-mission-token")
    public ResponseEntity<Map<String, String>> getSpecificMissionToken(
            Authentication authentication, 
            @RequestBody Map<String, Object> body) {
        try {
            String userId = authentication.getName();
            String missionId = (String) body.get("missionId");
            
            if (missionId == null || missionId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "missionId is required"));
            }
            
            // Generate token for specific mission channel
            String channel = "missions:" + missionId.trim();
            String token = centrifugoTokenService.issueSubscriptionToken(userId, channel);
            return ResponseEntity.ok(Map.of("subscriptionToken", token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}