package com.genepay.genepaypaymentservice.controller;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


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


}