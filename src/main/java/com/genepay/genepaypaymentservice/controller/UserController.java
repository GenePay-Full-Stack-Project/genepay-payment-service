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
    
    @PostMapping("/google-signin")
    @Operation(summary = "Google Sign-In", description = "Authenticate or register user using Google ID token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sign-in successful"),
            @ApiResponse(responseCode = "400", description = "Invalid Google token or email not verified")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<LoginResponse>> googleSignIn(
            @Valid @RequestBody GoogleSignInRequest request) {
        log.info("Google sign-in request received");
        LoginResponse response = userService.googleSignIn(request);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Sign-in successful", response));
    }
    
    @PostMapping("/{id}/link-face")
    @Operation(summary = "Link face biometric", description = "Link an enrolled face ID to the user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Face linked successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<UserResponse>> linkFace(
            @PathVariable("id") Long userId,
            @Valid @RequestBody LinkFaceRequest request) {
        log.info("Link face request received for user: {}", userId);
        UserResponse response = userService.linkFace(userId, request);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Face linked successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve user profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Get user request for: {}", id);
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("User retrieved successfully", user));
    }

    @PostMapping("/verify-token")
    @Operation(summary = "Verify JWT token", description = "Validate a JWT token and return its claims")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token verified")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<TokenVerifyResponse>> verifyToken(
            @Valid @RequestBody VerifyTokenRequest request) {
        log.info("Token verification request received");
        TokenVerifyResponse response = userService.verifyToken(request.getToken());
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Token verified", response));
    }

    @DeleteMapping("/{id}/delete-face")
    @Operation(summary = "Remove face biometric", description = "Remove the registered face biometric from a user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Face removed successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<UserResponse>> deleteFace(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Delete face request for user: {}", id);
        UserResponse user = userService.deleteFace(id);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Face biometric removed successfully", user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile", description = "Update user name and phone number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Update user request for: {}", id);
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("User updated successfully", user));
    }
}