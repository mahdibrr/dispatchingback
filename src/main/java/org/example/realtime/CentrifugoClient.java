// src/main/java/org/example/realtime/CentrifugoClient.java
package org.example.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CentrifugoClient {
    private static final Logger log = LoggerFactory.getLogger(CentrifugoClient.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient http;
    private final String baseUrl;

    public CentrifugoClient(
            @org.springframework.beans.factory.annotation.Value("${app.centrifugo.api-base}") String baseUrl,
            @org.springframework.beans.factory.annotation.Value("${app.centrifugo.api-key}") String apiKey
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = RestClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader("Authorization", "apikey " + apiKey)
                .build();
    }

    /* ---------------- Core HTTP publish ---------------- */

    public void publish(String channel, Map<String, Object> data) {
        try {
            var payload = Map.of("channel", channel, "data", data);
            // Log the payload to the console so terminal shows exact body sent to Centrifugo
            try {
                String json = JSON.writeValueAsString(payload);
                // Use both SLF4J and System.out to maximize visibility in different run contexts
                log.info("Centrifugo publish -> channel={} payload={}", channel, json);
                System.out.println("[Centrifugo publish] channel=" + channel + " payload=" + json);
            } catch (JsonProcessingException jpe) {
                log.info("Centrifugo publish -> channel={} payloadMap={} (failed to serialize)", channel, payload);
                System.out.println("[Centrifugo publish] channel=" + channel + " payloadMap=" + payload);
            }
            http.post()
                    .uri("/publish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Centrifugo publish OK channel={} type={}", channel, data.get("type"));
        } catch (Exception e) {
            log.error("Centrifugo publish FAILED channel={} err={}", channel, e.toString());
            throw e;
        }
    }

    /* ---------------- Channel helpers ---------------- */

    public String missionChannel(UUID missionId) {
        return "missions:" + missionId;
    }

    public String driverChannel(UUID driverId) {
        return "drivers:" + driverId;
    }

    public String statusChannel() {
        return "status";
    }

    /* ---------------- Unified mission event pipeline ----------------
       One channel per mission: missions:<missionId>.
       Event schema matches mobile/web:
       {
         "type": "location" | "status" | "assignment",
         "missionId": "<uuid>",
         "driverId": "<uuid>",
         "at": "<epoch_ms_string>",
         // location fields
         "lat": <double>, "lng": <double>, "accuracy": <float>,
         // status fields
         "status": "ASSIGNED|PICKED_UP|IN_TRANSIT|DELIVERED|CANCELLED",
         "assignedAt": "...", "pickedUpAt": "...", "inTransitAt": "...", "deliveredAt": "..."
       }
    ----------------------------------------------------------------- */

    public void publishMissionEvent(UUID missionId, Map<String, Object> event) {
        if (missionId == null) return;
        Map<String, Object> ev = new HashMap<>(event != null ? event : Map.of());
        ev.putIfAbsent("missionId", missionId.toString());
        ev.putIfAbsent("at", String.valueOf(System.currentTimeMillis()));
        publish(missionChannel(missionId), ev);
    }

    public void publishMissionLocation(UUID missionId, UUID driverId, double lat, double lng, float accuracy) {
        if (missionId == null) return;
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "location");
        ev.put("missionId", missionId.toString());
        if (driverId != null) ev.put("driverId", driverId.toString());
        ev.put("lat", lat);
        ev.put("lng", lng);
        ev.put("accuracy", accuracy);
        ev.put("at", String.valueOf(System.currentTimeMillis()));
        publish(missionChannel(missionId), ev);
    }

    public void publishMissionStatus(
            UUID missionId,
            UUID driverId,
            String status,
            String assignedAt,
            String pickedUpAt,
            String inTransitAt,
            String deliveredAt
    ) {
        if (missionId == null || status == null) return;
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "status");
        ev.put("missionId", missionId.toString());
        if (driverId != null) ev.put("driverId", driverId.toString());
        ev.put("status", status);
        if (assignedAt != null)  ev.put("assignedAt", assignedAt);
        if (pickedUpAt != null)  ev.put("pickedUpAt", pickedUpAt);
        if (inTransitAt != null) ev.put("inTransitAt", inTransitAt);
        if (deliveredAt != null) ev.put("deliveredAt", deliveredAt);
        ev.put("at", String.valueOf(System.currentTimeMillis()));
        publish(missionChannel(missionId), ev);
    }

    public void publishMissionAssigned(UUID missionId, UUID driverId, String initialStatus) {
        if (missionId == null) return;
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "assignment");
        ev.put("missionId", missionId.toString());
        if (driverId != null) ev.put("driverId", driverId.toString());
        if (initialStatus != null) ev.put("status", initialStatus);
        ev.put("at", String.valueOf(System.currentTimeMillis()));
        publish(missionChannel(missionId), ev);
    }

    /* ---------------- Optional: broadcast to global status stream ---------------- */

    public void publishGlobalStatus(UUID missionId, UUID driverId, String status) {
        if (status == null) return;
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "status");
        if (missionId != null) ev.put("missionId", missionId.toString());
        if (driverId != null)  ev.put("driverId", driverId.toString());
        ev.put("status", status);
        ev.put("at", String.valueOf(System.currentTimeMillis()));
        publish(statusChannel(), ev);
    }
}
