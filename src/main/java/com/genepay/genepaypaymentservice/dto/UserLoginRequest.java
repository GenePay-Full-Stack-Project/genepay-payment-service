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
public class UserLoginRequest {

    @NotBlank(message = "NIC number is required")
    private String nicNumber;

    @NotBlank(message = "Password is required")
    private String password;
}
