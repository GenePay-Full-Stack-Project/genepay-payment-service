package com.genepay.genepaypaymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCardRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must be 16 digits")
    private String cardNumber;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^\\d{3}$", message = "CVV must be 3 digits")
    private String cvv;

    @NotBlank(message = "Expiry is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Expiry must be in format MM/YY")
    private String expiry;

    @Size(max = 50, message = "Nickname cannot exceed 50 characters")
    private String nickname;

    private Boolean setAsDefault = false;
}