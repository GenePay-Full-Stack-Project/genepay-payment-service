package com.genepay.genepaypaymentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, one special character, and no whitespace"
    )
    private String password;

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 255, message = "Business name must be between 2 and 255 characters")
    private String businessName;

    @Size(min = 2, max = 100, message = "Owner name must be between 2 and 100 characters")
    private String ownerName;
    
    @jakarta.validation.constraints.Pattern(
        regexp = "^[+]?[0-9]{10,15}$",
        message = "Phone number must be between 10-15 digits and may start with +"
    )
    private String phoneNumber;
    
    @Size(max = 500, message = "Business address must not exceed 500 characters")
    private String businessAddress;
    
    @Size(max = 100, message = "Business type must not exceed 100 characters")
    private String businessType;
}