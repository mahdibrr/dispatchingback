package org.example.shared.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class CentrifugoApi {
    private final WebClient webClient;

    public CentrifugoApi(@Value("${app.centrifugo.api-base}") String apiBase,
                         @Value("${app.centrifugo.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(apiBase)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    public void publish(String channel, Object data) {
        try {
            webClient.post()
                    .uri("/publish")
                    .bodyValue(new PublishRequest(channel, data))
                    .retrieve()
            .bodyToMono(Void.class)
            .block();
        System.out.println("Centrifugo publish OK channel=" + channel + " payloadType=" + (data == null ? "null" : data.getClass().getSimpleName()));
        } catch (WebClientResponseException ex) {
        // Log minimal error info and response body for diagnostics (do not log secrets)
        String body = "";
        try { body = ex.getResponseBodyAsString(); } catch (Exception ignore) {}
        System.err.println("Centrifugo publish failed status=" + ex.getStatusCode() + " channel=" + channel + " body=" + body);
        } catch (Exception e) {
        System.err.println("Centrifugo publish error channel=" + channel + " msg=" + e.getMessage());
        }
    }

    private record PublishRequest(String channel, Object data) {}
}
