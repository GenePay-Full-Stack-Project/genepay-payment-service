package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.exception.BadRequestException;
import com.genepay.genepaypaymentservice.exception.BiometricVerificationException;
import com.genepay.genepaypaymentservice.exception.PaymentProcessingException;
import com.genepay.genepaypaymentservice.exception.ResourceNotFoundException;
import com.genepay.genepaypaymentservice.model.Merchant;
import com.genepay.genepaypaymentservice.model.Transaction;
import com.genepay.genepaypaymentservice.model.User;
import com.genepay.genepaypaymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final BankingServiceClient bankingServiceClient;
    private final CardService cardService;
    private final MerchantService merchantService;
    private final BiometricServiceClient biometricServiceClient;
    private final UserService userService;
    private final BlockchainAuditService blockchainAuditService;



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
    public PaymentVerifyResponse verifyAndCharge(PaymentVerifyRequest request) {
        log.info("Verifying and charging for transaction: {}", request.getTransactionId());

        // Get transaction
        Transaction transaction = transactionRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new BadRequestException("Transaction is not in pending state");
        }

        // Step 1: Identify user by face scan
        log.info("Identifying user by face for transaction: {}", request.getTransactionId());
        String userIdStr = biometricServiceClient.searchFace(request.getFaceData());
        
        if (userIdStr == null) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("User not identified - no matching face found");
            transactionRepository.save(transaction);
            throw new BiometricVerificationException("User identification failed - no matching face found");
        }
        
        Long userId = Long.parseLong(userIdStr);
        log.info("User identified: {} for transaction: {}", userId, request.getTransactionId());

        // Get merchant entity for audit logging
        MerchantResponse merchant = merchantService.getMerchantById(transaction.getMerchant().getId());
        log.info("Merchant for transaction {}: {}", request.getTransactionId(), merchant.getBusinessName());
        
        // Step 2: Get and validate user
        log.info("Validating user {} for transaction: {}", userId, request.getTransactionId());
        UserResponse userResponse = userService.getUserById(userId);
        if (!userResponse.getCardLinked()) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("User has not linked a payment card");
            transactionRepository.save(transaction);
            throw new BadRequestException("User has not linked a payment card");
        }
        if (!userResponse.getFaceEnrolled()) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("User has not enrolled face");
            transactionRepository.save(transaction);
            throw new BadRequestException("User has not enrolled face");
        }
        
        // Step 3: Link user to transaction
        User user = userService.findByFaceId(userResponse.getFaceId());
        transaction.setUser(user);
        transaction.setBiometricVerified(true);
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);
        
        log.info("User {} linked to transaction {}, proceeding with payment", userId, request.getTransactionId());

        // Process payment with Banking System
        try {
            // Get payment tokens from default cards
            String userToken = cardService.getUserDefaultPaymentToken(user.getId());
            String merchantToken = cardService.getMerchantDefaultPaymentToken(transaction.getMerchant().getId());
            String platformToken = bankingServiceClient.getPlatformPaymentToken();
            
            if (userToken == null || merchantToken == null) {
                throw new PaymentProcessingException("Missing payment tokens");
            }

            // Step 1: Transfer full amount from user to merchant via Banking System
            Boolean userToMerchantSuccess = bankingServiceClient.transferMoney(
                    userToken,
                    merchantToken,
                    transaction.getAmount(),
                    transaction.getDescription()
            );

            if (!userToMerchantSuccess) {
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transaction.setFailureReason("Payment transfer from user to merchant failed");
                transactionRepository.save(transaction);
                log.error("Payment failed for transaction: {}", transaction.getTransactionId());
                
                return PaymentVerifyResponse.builder()
                        .transactionId(transaction.getTransactionId())
                        .status(transaction.getStatus().name())
                        .verified(true)
                        .message("Payment failed")
                        .amount(transaction.getAmount())
                        .merchantName(transaction.getMerchant().getBusinessName())
                        .build();
            }

            // Step 2: Calculate and transfer 3% platform fee from merchant to BioPay platform
            java.math.BigDecimal platformFee = transaction.getAmount()
                    .multiply(java.math.BigDecimal.valueOf(0.03));
            java.math.BigDecimal merchantNetAmount = transaction.getAmount()
                    .multiply(java.math.BigDecimal.valueOf(0.97));

            if (platformToken != null && !platformToken.isEmpty()) {
                Boolean platformFeeSuccess = bankingServiceClient.transferMoney(
                        merchantToken,
                        platformToken,
                        platformFee,
                        "BioPay Platform Fee (3%) - Transaction: " + transaction.getTransactionId()
                );

                if (!platformFeeSuccess) {
                    log.warn("Platform fee transfer failed for transaction: {}. Fee amount: {} LKR", 
                            transaction.getTransactionId(), platformFee);
                    // Continue with transaction completion even if platform fee transfer fails
                    // This can be handled separately through reconciliation
                }
            } else {
                log.warn("BioPay platform payment token not configured. Platform fee of {} LKR not collected for transaction: {}",
                        platformFee, transaction.getTransactionId());
            }

            // Step 3: Update transaction status
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            
            // Update card last used timestamp
            cardService.updateCardLastUsed(userToken);
            
            log.info("Payment completed successfully: {} (Amount: {} LKR, Merchant Net: {} LKR, Platform Fee: {} LKR)", 
                    transaction.getTransactionId(), transaction.getAmount(), merchantNetAmount, platformFee);

            // Step 4: Record on blockchain audit ledger (async, non-blocking)
            try {
                // Record user -> merchant transaction (97% of amount)
                long merchantAmountCents = merchantNetAmount.multiply(java.math.BigDecimal.valueOf(100)).longValue();
                blockchainAuditService.recordTransactionAsync(
                        transaction.getTransactionId(),
                        merchantAmountCents,
                        user.getId(),
                        transaction.getMerchant().getId()
                );
                
                // Record platform fee transaction (3% of amount)
                long platformFeeCents = platformFee.multiply(java.math.BigDecimal.valueOf(100)).longValue();
                blockchainAuditService.recordPlatformFeeAsync(
                        transaction.getTransactionId(),
                        platformFeeCents,
                        merchant.getId()
                );
                
                log.info("Blockchain audit recording initiated for transaction: {}", transaction.getTransactionId());
            } catch (Exception e) {
                // Log error but don't fail the payment
                log.error("Failed to initiate blockchain audit for transaction {}: {}", 
                        transaction.getTransactionId(), e.getMessage());
            }

            return PaymentVerifyResponse.builder()
                    .transactionId(transaction.getTransactionId())
                    .status(transaction.getStatus().name())
                    .verified(true)
                    .message(transaction.getStatus() == Transaction.TransactionStatus.COMPLETED 
                            ? "Payment successful" 
                            : "Payment failed")
                    .amount(transaction.getAmount())
                    .merchantName(transaction.getMerchant().getBusinessName())
                    .build();

        } catch (Exception e) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage());
        }
    }


}