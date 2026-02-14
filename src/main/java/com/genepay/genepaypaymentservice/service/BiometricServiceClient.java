package com.genepay.genepaypaymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.biometric-service.base-url}")
    private String biometricServiceUrl;

    @Value("${app.biometric-service.timeout}")
    private Long timeout;

    public String searchFace(String faceData) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(biometricServiceUrl).build();
            
            Map<String, Object> request = Map.of(
                    "image_base64", faceData,
                    "top_k", 1,
                    "search_type", "user"
            );

            Map<String, Object> response = webClient.post()
                    .uri("/biometric/search")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofMillis(timeout))
                    .block();

            if (response != null && response.containsKey("matches")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> matches = 
                        (java.util.List<Map<String, Object>>) response.get("matches");
                
                if (!matches.isEmpty()) {
                    Map<String, Object> topMatch = matches.get(0);
                    Object userId = topMatch.get("user_id");
                    return userId != null ? userId.toString() : null;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error searching face with biometric service", e);
            throw new RuntimeException("Face search failed: " + e.getMessage());
        }
    }
}
