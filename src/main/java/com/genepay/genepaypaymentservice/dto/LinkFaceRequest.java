package com.genepay.genepaypaymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkFaceRequest {

    @NotBlank(message = "Face ID is required")
    private String faceId; // From biometric service
}
