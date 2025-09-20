package org.example.shared.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
// import org.springframework.stereotype.Service; // Removed @Service annotation to prevent bean conflict
import org.springframework.web.client.RestClient;

import java.util.Map;

// @Service // Commented out to prevent bean conflict with new CentrifugoClient
public class CentrifugoPublisher {

    private final RestClient http;
    private final String apiKey;

    public CentrifugoPublisher(
            @Value("${app.centrifugo.api-base}") String apiBase,      // e.g. https://centrifugo.example.com/api
            @Value("${app.centrifugo.api-key}") String apiKey) {      // keep secret
        // Use the API base URL as-is (should already include /api)
        this.http = RestClient.builder().baseUrl(apiBase).build();
        this.apiKey = apiKey;
    }

    public void publish(String channel, Object data) {
        try {
            System.out.println("Publishing to Centrifugo - Channel: " + channel + ", Base URL: " + http.toString());
            http.post()
                    .uri("/publish")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("channel", channel, "data", data))
                    .retrieve()
                    .toBodilessEntity();
            System.out.println("Centrifugo publish OK channel=" + channel + " payloadType=" + (data == null ? "null" : data.getClass().getSimpleName()));
        } catch (Exception e) {
            System.err.println("Centrifugo publish error channel=" + channel + " msg=" + e.getMessage());
        }
    }
}
