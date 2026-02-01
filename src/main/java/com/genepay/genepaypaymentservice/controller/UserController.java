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

    @PostMapping("/{userId}/link-face") //
    @Operation(summary = "Link face biometric", description = "Link face ID from biometric service to user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Face linked successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.biopay.paymentservice.dto.ApiResponse<UserResponse>> linkFace(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody LinkFaceRequest request) {
        log.info("Face linking request for user: {}", userId);
        UserResponse user = userService.linkFace(userId, request);
        return ResponseEntity.ok(com.biopay.paymentservice.dto.ApiResponse.success("Face linked successfully", user));
    }

    @GetMapping("/{userId}") //
    @Operation(summary = "Get user by ID", description = "Retrieve user profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.biopay.paymentservice.dto.ApiResponse<UserResponse>> getUser(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("Get user request for: {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(com.biopay.paymentservice.dto.ApiResponse.success(user));
    }

    @GetMapping("/email/{email}") //
    @Operation(summary = "Get user by email", description = "Retrieve user profile by email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.biopay.paymentservice.dto.ApiResponse<UserResponse>> getUserByEmail(
            @Parameter(description = "User email") @PathVariable String email) {
        log.info("Get user by email request for: {}", email);
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(com.biopay.paymentservice.dto.ApiResponse.success(user));
    }

    @GetMapping //
    @Operation(summary = "Get all users", description = "Retrieve list of all registered users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    public ResponseEntity<com.biopay.paymentservice.dto.ApiResponse<java.util.List<UserResponse>>> getAllUsers() {
        log.info("Get all users request");
        java.util.List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(com.biopay.paymentservice.dto.ApiResponse.success(users));
    }

    @PutMapping("/{userId}")//
    @Operation(summary = "Update user profile", description = "Update user profile information (all fields are optional)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate email/phone"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.biopay.paymentservice.dto.ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Update user request for: {}", userId);
        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(com.biopay.paymentservice.dto.ApiResponse.success("User updated successfully", user));
    }

    @PostMapping("/verify-token") //
    @Operation(summary = "Verify JWT token", description = "Verify if the JWT token is valid and get token information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token verification result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<com.biopay.paymentservice.dto.ApiResponse<TokenVerifyResponse>> verifyToken(
            @Valid @RequestBody VerifyTokenRequest request) {
        log.info("Token verification request");
        TokenVerifyResponse response = userService.verifyToken(request.getToken());
        return ResponseEntity.ok(com.biopay.paymentservice.dto.ApiResponse.success("Token verified", response));
    }

    @PostMapping("/refresh-token") //
    @Operation(summary = "Refresh JWT token", description = "Generate new access and refresh tokens using a valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.biopay.paymentservice.dto.ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        RefreshTokenResponse response = userService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(com.biopay.paymentservice.dto.ApiResponse.success("Token refreshed successfully", response));
    }

    @DeleteMapping("/{userId}/delete-face")//
    @Operation(summary = "Delete face biometric", description = "Delete face enrollment data for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Face deleted successfully"),
            @ApiResponse(responseCode = "400", description = "No face enrolled for this user"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.biopay.paymentservice.dto.ApiResponse<UserResponse>> deleteFace(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("Delete face request for user: {}", userId);
        UserResponse user = userService.deleteFace(userId);
        return ResponseEntity.ok(com.biopay.paymentservice.dto.ApiResponse.success("Face deleted successfully", user));
    }
    
}