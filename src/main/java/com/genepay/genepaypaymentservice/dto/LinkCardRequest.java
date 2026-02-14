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
public class LinkCardRequest {

    @NotBlank(message = "Card number is required")
    private String cardNumber; // 16-digit card number
    
    @NotBlank(message = "CVV is required")
    private String cvv;
    
    @NotBlank(message = "Expiry date is required")
    private String expiry; // MM/YY format
}
