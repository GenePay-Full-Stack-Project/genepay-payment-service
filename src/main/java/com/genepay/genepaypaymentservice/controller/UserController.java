package com.genepay.genepaypaymentservice.controller;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user registration, authentication, and profile management")
public class UserController {
    private final UserService userService;

    @PostMapping("/send-verification-code")
    @Operation(summary = "Send verification code", description = "Send verification code to email before registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification code sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        log.info("Send verification code request for: {}", request.getEmail());
        userService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Verification code sent successfully", null));
    }
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input, user already exists, or invalid verification code")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        log.info("User registration request received for: {}", request.getEmail());
        UserResponse user = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.genepay.genepaypaymentservice.dto.ApiResponse.success("User registered successfully", user));
    }
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user using NIC number and password, receive JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<LoginResponse>> loginUser(
            @Valid @RequestBody UserLoginRequest request) {
        log.info("User login request received for: {}", request.getNicNumber());
        LoginResponse response = userService.loginUser(request);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Login successful", response));
    }
    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email", description = "Verify user email address using verification code before registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification code")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        log.info("Email verification request for: {}", request.getEmail());
        userService.verifyEmail(request);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Email verified successfully", null));
    }
    
}