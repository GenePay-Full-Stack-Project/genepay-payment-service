package com.genepay.genepaypaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to record transaction on blockchain audit ledger
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainAuditRequest {
    
    private String txIdOffchain;  // Transaction ID from payment service
    private Long amount;          // Amount in cents
    private Long timestamp;       // Unix timestamp
    private String fromId;        // User ID
    private String toId;          // Merchant ID or "platform"
}
