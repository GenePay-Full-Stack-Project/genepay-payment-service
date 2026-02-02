package com.genepay.genepaypaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {
    private String transactionId;
    private String sessionId; // Used for face verification
    private String status;
    private String message;
}