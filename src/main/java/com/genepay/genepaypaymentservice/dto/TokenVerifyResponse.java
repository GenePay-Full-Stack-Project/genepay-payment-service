package com.genepay.genepaypaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerifyResponse {
    private Boolean valid;
    private String email;
    private Long userId;
    private String userType;
    private Long expiresAt;
}
