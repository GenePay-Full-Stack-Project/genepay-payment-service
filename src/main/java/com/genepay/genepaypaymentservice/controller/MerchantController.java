package com.genepay.genepaypaymentservice.controller;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

@Slf4j
@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant Management", description = "APIs for merchant registration, authentication, and profile management")
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/send-verification-code")
    @Operation(summary = "Send verification code", description = "Send verification code to merchant email before registration")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification code sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email or email already registered")
    })
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        log.info("Send verification code request for merchant: {}", request.getEmail());
        merchantService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.<Void>success("Verification code sent to email", null));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify merchant email", description = "Verify merchant email address using verification code before registration")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired verification code")
    })
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        log.info("Verify email request for merchant: {}", request.getEmail());
        merchantService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.<Void>success("Email verified successfully", null));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new merchant", description = "Create a new merchant account (requires email verification)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Merchant registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input, merchant already exists, or email not verified")
    })
    public ResponseEntity<ApiResponse<MerchantResponse>> registerMerchant(
            @Valid @RequestBody MerchantRegistrationRequest request) {
        log.info("Merchant registration request received for: {}", request.getEmail());
        MerchantResponse merchant = merchantService.registerMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Merchant registered successfully", merchant));
    }

    @PostMapping("/login")
    @Operation(summary = "Merchant login", description = "Authenticate merchant using email and password, receive JWT token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> loginMerchant(
            @Valid @RequestBody LoginRequest request) {
        log.info("Merchant login request received for: {}", request.getEmail());
        LoginResponse response = merchantService.loginMerchant(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/{merchantId}")
    @Operation(summary = "Get merchant by ID", description = "Retrieve merchant profile information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merchant found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merchant not found")
    })
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchant(
            @Parameter(description = "Merchant ID") @PathVariable Long merchantId) {
        log.info("Get merchant request for: {}", merchantId);
        MerchantResponse merchant = merchantService.getMerchantById(merchantId);
        return ResponseEntity.ok(ApiResponse.success(merchant));
    }

    @PutMapping("/{merchantId}")
    @Operation(summary = "Update merchant profile", description = "Update merchant profile information (all fields are optional)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merchant updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or duplicate email/phone/business name"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merchant not found")
    })
    public ResponseEntity<ApiResponse<MerchantResponse>> updateMerchant(
            @Parameter(description = "Merchant ID") @PathVariable Long merchantId,
            @Valid @RequestBody UpdateMerchantRequest request) {
        log.info("Update merchant request for: {}", merchantId);
        MerchantResponse merchant = merchantService.updateMerchant(merchantId, request);
        return ResponseEntity.ok(ApiResponse.success("Merchant updated successfully", merchant));
    }

    @PostMapping("/verify-token")
    @Operation(summary = "Verify JWT token", description = "Verify if the JWT token is valid and get token information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token verification result returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<TokenVerifyResponse>> verifyToken(
            @Valid @RequestBody VerifyTokenRequest request) {
        log.info("Merchant token verification request");
        TokenVerifyResponse response = merchantService.verifyToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Token verified", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT token", description = "Generate new access and refresh tokens using a valid refresh token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merchant not found")
    })
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Merchant token refresh request");
        RefreshTokenResponse response = merchantService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
}