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

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get merchant transactions", description = "Retrieve paginated transaction history for a merchant")
    public ResponseEntity<com.genepay.genepaypaymentservice.dto.ApiResponse<Page<TransactionResponse>>> getMerchantTransactions(
            @Parameter(description = "Merchant ID") @PathVariable Long merchantId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.info("Get transactions for merchant: {}", merchantId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> transactions = paymentService.getMerchantTransactions(merchantId, pageable);
        return ResponseEntity.ok(com.genepay.genepaypaymentservice.dto.ApiResponse.success(transactions));
    }

}