package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.model.Transaction;
import com.genepay.genepaypaymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformService {

    private final TransactionRepository transactionRepository;
    private final BankingServiceClient bankingServiceClient;

    /**
     * Get platform balance (3% fees collected)
     */
    public Map<String, Object> getPlatformBalance() {
        log.info("Calculating platform balance");

        // Get all completed transactions
        List<Transaction> completedTransactions = transactionRepository
                .findByStatus(Transaction.TransactionStatus.COMPLETED);

        // Calculate total fees (3% of each transaction)
        BigDecimal totalFees = completedTransactions.stream()
                .map(t -> t.getAmount().multiply(BigDecimal.valueOf(0.03)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get total transaction volume
        BigDecimal totalVolume = completedTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new HashMap<>();
        response.put("totalFeesCollected", totalFees);
        response.put("totalTransactionVolume", totalVolume);
        response.put("transactionCount", completedTransactions.size());
        response.put("averageTransactionSize", completedTransactions.isEmpty() ? 
                BigDecimal.ZERO : totalVolume.divide(BigDecimal.valueOf(completedTransactions.size()), 2, BigDecimal.ROUND_HALF_UP));
        response.put("currency", "LKR");

        return response;
    }

    /**
     * Get fee summary for a date range
     */
    public Map<String, Object> getFeeSummary(String startDateStr, String endDateStr) {
        log.info("Generating fee summary from {} to {}", startDateStr, endDateStr);

        LocalDateTime startDate = startDateStr != null ? 
                LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE).atStartOfDay() : 
                LocalDateTime.now().minusMonths(1);

        LocalDateTime endDate = endDateStr != null ? 
                LocalDate.parse(endDateStr, DateTimeFormatter.ISO_DATE).atTime(23, 59, 59) : 
                LocalDateTime.now();

        List<Transaction> transactions = transactionRepository
                .findByStatusAndCreatedAtBetween(
                        Transaction.TransactionStatus.COMPLETED, 
                        startDate, 
                        endDate
                );

        BigDecimal totalFees = transactions.stream()
                .map(t -> t.getAmount().multiply(BigDecimal.valueOf(0.03)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVolume = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new HashMap<>();
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("totalFeesCollected", totalFees);
        response.put("totalTransactionVolume", totalVolume);
        response.put("transactionCount", transactions.size());
        response.put("averageFeePerTransaction", transactions.isEmpty() ? 
                BigDecimal.ZERO : totalFees.divide(BigDecimal.valueOf(transactions.size()), 2, BigDecimal.ROUND_HALF_UP));
        response.put("currency", "LKR");

        return response;
    }

    /**
     * Get platform statistics
     */
    public Map<String, Object> getPlatformStatistics(String period) {
        log.info("Generating platform statistics for period: {}", period);

        LocalDateTime startDate = switch (period.toLowerCase()) {
            case "day" -> LocalDateTime.now().minusDays(1);
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            case "year" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.of(2020, 1, 1, 0, 0); // All time
        };

        List<Transaction> transactions = transactionRepository
                .findByStatusAndCreatedAtAfter(Transaction.TransactionStatus.COMPLETED, startDate);

        BigDecimal totalFees = transactions.stream()
                .map(t -> t.getAmount().multiply(BigDecimal.valueOf(0.03)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVolume = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Count unique users and merchants
        long uniqueUsers = transactions.stream()
                .map(t -> t.getUser().getId())
                .distinct()
                .count();

        long uniqueMerchants = transactions.stream()
                .map(t -> t.getMerchant().getId())
                .distinct()
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("period", period);
        response.put("startDate", startDate);
        response.put("totalFeesCollected", totalFees);
        response.put("totalTransactionVolume", totalVolume);
        response.put("transactionCount", transactions.size());
        response.put("uniqueUsers", uniqueUsers);
        response.put("uniqueMerchants", uniqueMerchants);
        response.put("currency", "LKR");

        return response;
    }
}
