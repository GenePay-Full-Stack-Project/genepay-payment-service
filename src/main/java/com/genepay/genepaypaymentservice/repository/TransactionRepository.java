package com.genepay.genepaypaymentservice.repository;

import com.genepay.genepaypaymentservice.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // REQUIRED FOR REFUND: Find transaction by transaction ID
    Optional<Transaction> findByTransactionId(String transactionId);

    // Fetch transaction with user and merchant details
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.merchant WHERE t.transactionId = :transactionId")
    Optional<Transaction> findByTransactionIdWithDetails(@Param("transactionId") String transactionId);

    Optional<Transaction> findByBankingTransactionId(String bankingTransactionId);

    // User and merchant transaction queries
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.merchant WHERE t.user.id = :userId")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.merchant WHERE t.merchant.id = :merchantId ORDER BY t.createdAt DESC")
    Page<Transaction> findByMerchantId(@Param("merchantId") Long merchantId, Pageable pageable);

    // REQUIRED FOR REFUND: Find transactions by user and status
    List<Transaction> findByUserIdAndStatus(Long userId, Transaction.TransactionStatus status);

    List<Transaction> findByMerchantIdAndStatus(Long merchantId, Transaction.TransactionStatus status);

    // Date range queries
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.merchant.id = :merchantId AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByMerchantIdAndDateRange(
            @Param("merchantId") Long merchantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Stale transaction cleanup
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt < :beforeDate")
    List<Transaction> findStaleTransactions(
            @Param("status") Transaction.TransactionStatus status,
            @Param("beforeDate") LocalDateTime beforeDate
    );

    // Platform fee and status queries
    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    List<Transaction> findByStatusAndCreatedAtBetween(
            Transaction.TransactionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<Transaction> findByStatusAndCreatedAtAfter(
            Transaction.TransactionStatus status,
            LocalDateTime startDate
    );
}