package com.genepay.genepaypaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private Long userId;
    private String userName;
    private Long merchantId;
    private String merchantName;
    private BigDecimal amount;
    private String currency;
    private String status;  // Can be "REFUNDED"
    private String type;
    private String description;
    private Boolean biometricVerified;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}