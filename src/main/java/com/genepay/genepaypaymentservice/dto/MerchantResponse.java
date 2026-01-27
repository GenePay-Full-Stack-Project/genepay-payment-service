package com.genepay.genepaypaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponse {
    private Long id;
    private String email;
    private String businessName;
    private String ownerName;
    private String phoneNumber;
    private String businessAddress;
    private String businessType;
    private String status;
    private Boolean cardLinked;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}