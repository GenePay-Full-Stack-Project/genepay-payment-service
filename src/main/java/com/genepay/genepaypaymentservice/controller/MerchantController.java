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
}