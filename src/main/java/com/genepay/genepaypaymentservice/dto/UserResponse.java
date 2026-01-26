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
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String nicNumber;
    private String phoneNumber;
    private String faceId;
    private BigDecimal balance;
    private String status;
    private Boolean emailVerified;
    private Boolean faceEnrolled;
    private Boolean cardLinked;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
