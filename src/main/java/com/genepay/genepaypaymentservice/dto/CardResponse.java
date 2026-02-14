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
public class CardResponse {
    private Long id;
    private String cardLast4;
    private String cardBrand;
    private String expiryMonth;
    private String expiryYear;
    private Boolean isDefault;
    private Boolean isActive;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
