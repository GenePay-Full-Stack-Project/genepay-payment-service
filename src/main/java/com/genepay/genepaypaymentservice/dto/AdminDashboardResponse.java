package com.genepay.genepaypaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    
    // User statistics
    private Long totalUsers;
    private Long activeUsers;
    private Long suspendedUsers;
    private Long usersWithFaceEnrolled;
    private Long usersWithCardLinked;
    
    // Merchant statistics
    private Long totalMerchants;
    private Long activeMerchants;
    private Long pendingMerchants;
    private Long suspendedMerchants;
    
    // Transaction statistics
    private Long totalTransactions;
    private Long completedTransactions;
    private Long pendingTransactions;
    private Long failedTransactions;
    
    // Financial statistics
    private BigDecimal totalTransactionVolume;
    private BigDecimal totalPlatformFees;
    private BigDecimal pendingPlatformFees;
    private BigDecimal collectedPlatformFees;
    
    // Recent activity
    private Long transactionsToday;
    private Long transactionsThisWeek;
    private Long transactionsThisMonth;
    
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;
    
    private Long newMerchantsToday;
    private Long newMerchantsThisWeek;
    private Long newMerchantsThisMonth;
}
