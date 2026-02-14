package com.genepay.genepaypaymentservice.controller;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "APIs for admin authentication and dashboard management")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate admin using email and password, receive JWT token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> loginAdmin(
            @Valid @RequestBody AdminLoginRequest request) {
        log.info("Admin login request received for: {}", request.getEmail());
        LoginResponse response = adminService.loginAdmin(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/{adminId}")
    @Operation(summary = "Get admin by ID", description = "Retrieve admin profile information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Admin not found")
    })
    public ResponseEntity<ApiResponse<AdminResponse>> getAdmin(
            @Parameter(description = "Admin ID") @PathVariable Long adminId) {
        log.info("Get admin request for: {}", adminId);
        AdminResponse admin = adminService.getAdminById(adminId);
        return ResponseEntity.ok(ApiResponse.success(admin));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics", description = "Retrieve comprehensive dashboard statistics including users, merchants, transactions, and platform revenue")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        log.info("Get dashboard statistics request");
        AdminDashboardResponse dashboard = adminService.getDashboardStatistics();
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", dashboard));
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Retrieve list of all registered users for admin panel")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Admin get all users request");
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/merchants")
    @Operation(summary = "Get all merchants", description = "Retrieve list of all registered merchants for admin panel")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merchants retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<MerchantResponse>>> getAllMerchants() {
        log.info("Admin get all merchants request");
        List<MerchantResponse> merchants = adminService.getAllMerchants();
        return ResponseEntity.ok(ApiResponse.success("Merchants retrieved successfully", merchants));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get all transactions", description = "Retrieve list of all transactions for admin panel")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions() {
        log.info("Admin get all transactions request");
        List<TransactionResponse> transactions = adminService.getAllTransactions();
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", transactions));
    }
}
