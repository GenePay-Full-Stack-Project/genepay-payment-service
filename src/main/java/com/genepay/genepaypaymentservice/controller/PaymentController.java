package com.genepay.genepaypaymentservice.controller;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Processing", description = "APIs for payment initiation, verification, and transaction management")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{transactionId}/refund")
    @Operation(summary = "Refund transaction", description = "Process a refund for a completed transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction refunded"),
            @ApiResponse(responseCode = "400", description = "Cannot refund transaction in current state")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<TransactionResponse>> refundTransaction(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId,
            @Parameter(description = "Refund reason") @RequestParam String reason) {
        log.info("Refund request for transaction: {}", transactionId);
        TransactionResponse transaction = paymentService.refundTransaction(transactionId, reason);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Transaction refunded", transaction));
    }


    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment", description = "Create a pending payment transaction that requires biometric verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or merchant not ready for payments")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<PaymentInitiateResponse>> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request) {
        log.info("Payment initiation request for merchant {}", request.getMerchantId());
        PaymentInitiateResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Payment initiated", response));
    }


    @PostMapping("/verify")
    @Operation(summary = "Verify and charge payment", description = "Verify user identity via biometric and complete payment via Banking System")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed"),
            @ApiResponse(responseCode = "401", description = "Biometric verification failed"),
            @ApiResponse(responseCode = "500", description = "Payment processing error")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<PaymentVerifyResponse>> verifyAndCharge(
            @Valid @RequestBody PaymentVerifyRequest request) {
        log.info("Payment verification request for transaction: {}", request.getTransactionId());
        PaymentVerifyResponse response = paymentService.verifyAndCharge(request);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Payment processed", response));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction details", description = "Retrieve transaction information by transaction ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<TransactionResponse>> getTransaction(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {
        log.info("Get transaction request for: {}", transactionId);
        TransactionResponse transaction = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success(transaction));
    }


    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user transactions", description = "Retrieve paginated transaction history for a user")
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<Page<TransactionResponse>>> getUserTransactions(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.info("Get transactions for user: {}", userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> transactions = paymentService.getUserTransactions(userId, pageable);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success(transactions));
    }


    @GetMapping("/user/{userId}/total-spends")
    @Operation(summary = "Get user total spends", description = "Calculate total amount spent by user from completed transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total spends calculated successfully")
    })
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<Double>> getUserTotalSpends(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("Get total spends for user: {}", userId);
        Double total = paymentService.getUserTotalSpends(userId);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success("Total spends calculated", total));
    }



}