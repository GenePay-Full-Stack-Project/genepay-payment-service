package com.genepay.genepaypaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from blockchain relay service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainAuditResponse {
    
    private String status;
    private String message;
    private BlockchainData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockchainData {
        private String txIdOffchain;
        private String blockchainTxHash;
        private Long blockNumber;
        private String gasUsed;
        private String dataHash;
    }
}
