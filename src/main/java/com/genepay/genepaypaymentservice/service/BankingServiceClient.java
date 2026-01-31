package com.genepay.genepaypaymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankingServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.banking-service.base-url:http://localhost:5000}")
    private String bankingServiceUrl;

    @Value("${app.banking-service.timeout:5000}")
    private Long timeout;

    @Value("${app.banking-service.platform-payment-token:}")
    private String platformPaymentToken;

    /**
     * Transfer money from sender to receiver using payment tokens
     * @param senderToken Payment token of sender
     * @param receiverToken Payment token of receiver
     * @param amount Amount to transfer (LKR)
     * @param description Transaction description
     * @return true if successful, false otherwise
     */
    public Boolean transferMoney(String senderToken, String receiverToken, BigDecimal amount, String description) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(bankingServiceUrl).build();

            Map<String, Object> request = Map.of(
                    "senderToken", senderToken,
                    "receiverToken", receiverToken,
                    "amount", amount,
                    "description", description != null ? description : "Payment"
            );

            Map<String, Object> response = webClient.post()
                    .uri("/api/external/transfer")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofMillis(timeout))
                    .block();

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.info("Transfer successful: {} LKR from {} to {}", amount, senderToken, receiverToken);
                return true;
            }

            log.warn("Transfer failed: {}", response != null ? response.get("message") : "Unknown error");
            return false;
        } catch (Exception e) {
            log.error("Failed to transfer money via banking service: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get BioPay platform payment token
     * @return Platform payment token
     */
    public String getPlatformPaymentToken() {
        return platformPaymentToken;
    }
}