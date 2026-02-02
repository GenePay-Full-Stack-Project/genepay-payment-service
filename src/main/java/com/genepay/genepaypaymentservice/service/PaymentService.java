package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.exception.BadRequestException;
import com.genepay.genepaypaymentservice.exception.PaymentProcessingException;
import com.genepay.genepaypaymentservice.exception.ResourceNotFoundException;
import com.genepay.genepaypaymentservice.model.Merchant;
import com.genepay.genepaypaymentservice.model.Transaction;
import com.genepay.genepaypaymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final BankingServiceClient bankingServiceClient;
    private final CardService cardService;

    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) {
        log.info("Initiating payment for merchant {}", request.getMerchantId());

        // Validate merchant
        MerchantResponse merchantResponse = merchantService.getMerchantById(request.getMerchantId());
        if (!merchantResponse.getCardLinked()) {
            throw new BadRequestException("Merchant has not linked a payment card");
        }

        // Get merchant entity
        Merchant merchant = merchantService.getMerchantEntityById(request.getMerchantId());

        // Create transaction record WITHOUT user - user will be identified during verification
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .user(null)  // User will be linked during face verification
                .merchant(merchant)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.PAYMENT)
                .biometricVerified(false)
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Payment initiated with transaction ID: {} (User will be identified during verification)", transaction.getTransactionId());

        return PaymentInitiateResponse.builder()
                .transactionId(transaction.getTransactionId())
                .sessionId(transaction.getTransactionId())
                .status("PENDING")
                .message("Please scan customer's face to identify and process payment")
                .build();
    }

    @Transactional
    public TransactionResponse refundTransaction(String transactionId, String reason) {
        log.info("Refunding transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (transaction.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new BadRequestException("Only completed transactions can be refunded");
        }

        try {
            // Reverse the transfer in Banking System
            String merchantToken = cardService.getMerchantDefaultPaymentToken(transaction.getMerchant().getId());
            String userToken = cardService.getUserDefaultPaymentToken(transaction.getUser().getId());

            // Step 1: Transfer platform fee back from BioPay platform to merchant (if platform token is configured)
            java.math.BigDecimal platformFee = transaction.getAmount()
                    .multiply(java.math.BigDecimal.valueOf(0.03));
            String platformToken = bankingServiceClient.getPlatformPaymentToken();

            if (platformToken != null && !platformToken.isEmpty()) {
                Boolean platformFeeRefundSuccess = bankingServiceClient.transferMoney(
                        platformToken,
                        merchantToken,
                        platformFee,
                        "BioPay Platform Fee Refund (3%) - Transaction: " + transactionId
                );

                if (!platformFeeRefundSuccess) {
                    log.warn("Platform fee refund failed for transaction: {}. Fee amount: {} LKR",
                            transactionId, platformFee);
                }
            }

            // Step 2: Transfer full amount from merchant back to user
            Boolean refundSuccess = bankingServiceClient.transferMoney(
                    merchantToken,
                    userToken,
                    transaction.getAmount(),
                    "Refund: " + reason
            );

            if (refundSuccess) {
                transaction.setStatus(Transaction.TransactionStatus.REFUNDED);
                transaction.setFailureReason(reason);
                transactionRepository.save(transaction);

                log.info("Transaction refunded successfully: {} (Amount: {} LKR, Platform Fee Returned: {} LKR)",
                        transactionId, transaction.getAmount(), platformFee);
            } else {
                throw new PaymentProcessingException("Refund transfer failed");
            }

            return mapToTransactionResponse(transaction);

        } catch (Exception e) {
            log.error("Failed to refund transaction: {}", transactionId, e);
            throw new PaymentProcessingException("Refund processing failed: " + e.getMessage());
        }
    }
    public Page<TransactionResponse> getMerchantTransactions(Long merchantId, Pageable pageable) {
        return transactionRepository.findByMerchantId(merchantId, pageable)
                .map(this::mapToTransactionResponse);
    }



    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUser() != null ? transaction.getUser().getId() : null)
                .userName(transaction.getUser() != null ? transaction.getUser().getFullName() : "Pending Identification")
                .merchantId(transaction.getMerchant().getId())
                .merchantName(transaction.getMerchant().getBusinessName())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .type(transaction.getType().name())
                .description(transaction.getDescription())
                .biometricVerified(transaction.getBiometricVerified())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}