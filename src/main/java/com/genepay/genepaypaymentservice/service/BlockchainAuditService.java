package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.BlockchainAuditRequest;
import com.genepay.genepaypaymentservice.dto.BlockchainAuditResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Service to interact with blockchain relay for audit trail
 */
@Slf4j
@Service
public class BlockchainAuditService {

    private final WebClient webClient;
    private final boolean blockchainEnabled;

    public BlockchainAuditService(
            WebClient.Builder webClientBuilder,
            @Value("${app.blockchain.relay.url:http://localhost:3001}") String relayUrl,
            @Value("${app.blockchain.enabled:true}") boolean blockchainEnabled) {
        
        this.webClient = webClientBuilder
                .baseUrl(relayUrl)
                .build();
        this.blockchainEnabled = blockchainEnabled;
        
        log.info("Blockchain Audit Service initialized - Enabled: {}, Relay URL: {}", 
                blockchainEnabled, relayUrl);
    }

    /**
     * Record transaction on blockchain (non-blocking)
     * @param transactionId Payment service transaction ID
     * @param amount Amount in cents
     * @param userId User ID
     * @param merchantId Merchant ID
     */
    public void recordTransactionAsync(String transactionId, Long amount, Long userId, Long merchantId) {
        if (!blockchainEnabled) {
            log.debug("Blockchain audit is disabled, skipping transaction: {}", transactionId);
            return;
        }

        BlockchainAuditRequest request = BlockchainAuditRequest.builder()
                .txIdOffchain(transactionId)
                .amount(amount)
                .timestamp(System.currentTimeMillis() / 1000)
                .fromId("user_" + userId)
                .toId("merchant_" + merchantId)
                .build();

        recordTransaction(request)
                .subscribe(
                        response -> log.info("✅ Blockchain audit recorded: {} - TX Hash: {}", 
                                transactionId, 
                                response.getData() != null ? response.getData().getBlockchainTxHash() : "N/A"),
                        error -> log.error("❌ Failed to record blockchain audit for {}: {}", 
                                transactionId, error.getMessage())
                );
    }

    /**
     * Record platform fee transaction on blockchain
     * @param transactionId Transaction ID
     * @param feeAmount Fee amount in cents
     * @param userId User ID
     */
    public void recordPlatformFeeAsync(String transactionId, Long feeAmount, Long userId) {
        if (!blockchainEnabled) {
            log.debug("Blockchain audit is disabled, skipping fee transaction: {}", transactionId);
            return;
        }

        String feeTransactionId = transactionId + "_FEE";
        
        BlockchainAuditRequest request = BlockchainAuditRequest.builder()
                .txIdOffchain(feeTransactionId)
                .amount(feeAmount)
                .timestamp(System.currentTimeMillis() / 1000)
                .fromId("merchant_" + userId)
                .toId("platform")
                .build();

        recordTransaction(request)
                .subscribe(
                        response -> log.info("✅ Platform fee audit recorded: {} - TX Hash: {}", 
                                feeTransactionId,
                                response.getData() != null ? response.getData().getBlockchainTxHash() : "N/A"),
                        error -> log.error("❌ Failed to record platform fee audit for {}: {}", 
                                feeTransactionId, error.getMessage())
                );
    }

    /**
     * Record transaction on blockchain with retry logic
     * @param request Blockchain audit request
     * @return Mono of BlockchainAuditResponse
     */
    private Mono<BlockchainAuditResponse> recordTransaction(BlockchainAuditRequest request) {
        return webClient.post()
                .uri("/record-transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BlockchainAuditResponse.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException) ||
                                ((WebClientResponseException) throwable).getStatusCode() != HttpStatus.CONFLICT)
                        .doBeforeRetry(retrySignal -> 
                                log.warn("Retrying blockchain recording (attempt {}): {}", 
                                        retrySignal.totalRetries() + 1, request.getTxIdOffchain()))
                )
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException webError = (WebClientResponseException) error;
                        log.error("Blockchain relay error [{}]: {}", 
                                webError.getStatusCode(), 
                                webError.getResponseBodyAsString());
                    }
                });
    }

    /**
     * Check blockchain relay health
     * @return true if relay is healthy, false otherwise
     */
    public boolean isRelayHealthy() {
        if (!blockchainEnabled) {
            return false;
        }

        try {
            return webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .map(response -> response.contains("\"status\":\"ok\""))
                    .onErrorReturn(false)
                    .block();
        } catch (Exception e) {
            log.warn("Blockchain relay health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get blockchain statistics
     * @return Statistics from blockchain
     */
    public Mono<String> getBlockchainStats() {
        return webClient.get()
                .uri("/stats")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10));
    }
}
