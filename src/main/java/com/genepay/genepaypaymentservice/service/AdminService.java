package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.exception.ResourceNotFoundException;
import com.genepay.genepaypaymentservice.exception.UnauthorizedException;
import com.genepay.genepaypaymentservice.model.Admin;
import com.genepay.genepaypaymentservice.model.Merchant;
import com.genepay.genepaypaymentservice.model.Transaction;
import com.genepay.genepaypaymentservice.model.User;
import com.genepay.genepaypaymentservice.repository.AdminRepository;
import com.genepay.genepaypaymentservice.repository.MerchantRepository;
import com.genepay.genepaypaymentservice.repository.TransactionRepository;
import com.genepay.genepaypaymentservice.repository.UserRepository;
import com.genepay.genepaypaymentservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;

    @Transactional
    public LoginResponse loginAdmin(AdminLoginRequest request) {
        log.info("Admin login attempt: {}", request.getEmail());

        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if account is locked
        if (admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException("Account is locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            handleFailedLogin(admin);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check admin status
        if (admin.getStatus() != Admin.AdminStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }

        // Reset failed attempts
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        // Generate tokens
        String token = jwtUtil.generateToken(admin.getEmail(), "ADMIN", admin.getId());
        String refreshToken = jwtUtil.generateRefreshToken(admin.getEmail(), "ADMIN", admin.getId());

        log.info("Admin logged in successfully: {}", admin.getId());

        // Create UserResponse for consistency with other login responses
        UserResponse adminAsUser = UserResponse.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFirstName() + " " + admin.getLastName())
                .build();

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L) // 24 hours
                .user(adminAsUser)
                .build();
    }

    public AdminResponse getAdminById(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        return modelMapper.map(admin, AdminResponse.class);
    }

    public AdminDashboardResponse getDashboardStatistics() {
        log.info("Fetching admin dashboard statistics");

        // Get all data
        List<User> allUsers = userRepository.findAll();
        List<Merchant> allMerchants = merchantRepository.findAll();
        List<Transaction> allTransactions = transactionRepository.findAll();

        // Calculate date ranges
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().minusDays(30).atStartOfDay();

        // User statistics
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(u -> u.getStatus() == User.UserStatus.ACTIVE).count();
        long suspendedUsers = allUsers.stream().filter(u -> u.getStatus() == User.UserStatus.SUSPENDED).count();
        long usersWithFaceEnrolled = allUsers.stream().filter(User::getFaceEnrolled).count();
        long usersWithCardLinked = allUsers.stream().filter(User::getCardLinked).count();
        
        long newUsersToday = allUsers.stream().filter(u -> u.getCreatedAt().isAfter(todayStart)).count();
        long newUsersThisWeek = allUsers.stream().filter(u -> u.getCreatedAt().isAfter(weekStart)).count();
        long newUsersThisMonth = allUsers.stream().filter(u -> u.getCreatedAt().isAfter(monthStart)).count();

        // Merchant statistics
        long totalMerchants = allMerchants.size();
        long activeMerchants = allMerchants.stream().filter(m -> m.getStatus() == Merchant.MerchantStatus.ACTIVE).count();
        long pendingMerchants = allMerchants.stream().filter(m -> m.getStatus() == Merchant.MerchantStatus.PENDING).count();
        long suspendedMerchants = allMerchants.stream().filter(m -> m.getStatus() == Merchant.MerchantStatus.SUSPENDED).count();
        
        long newMerchantsToday = allMerchants.stream().filter(m -> m.getCreatedAt().isAfter(todayStart)).count();
        long newMerchantsThisWeek = allMerchants.stream().filter(m -> m.getCreatedAt().isAfter(weekStart)).count();
        long newMerchantsThisMonth = allMerchants.stream().filter(m -> m.getCreatedAt().isAfter(monthStart)).count();

        // Transaction statistics
        long totalTransactions = allTransactions.size();
        long completedTransactions = allTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED).count();
        long pendingTransactions = allTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING || 
                            t.getStatus() == Transaction.TransactionStatus.PROCESSING).count();
        long failedTransactions = allTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.FAILED).count();
        
        long transactionsToday = allTransactions.stream().filter(t -> t.getCreatedAt().isAfter(todayStart)).count();
        long transactionsThisWeek = allTransactions.stream().filter(t -> t.getCreatedAt().isAfter(weekStart)).count();
        long transactionsThisMonth = allTransactions.stream().filter(t -> t.getCreatedAt().isAfter(monthStart)).count();

        // Financial statistics (Platform fee is 2% of transaction amount)
        BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.02");
        
        BigDecimal totalTransactionVolume = allTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPlatformFees = totalTransactionVolume.multiply(PLATFORM_FEE_PERCENTAGE);

        BigDecimal pendingTransactionVolume = allTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING || 
                            t.getStatus() == Transaction.TransactionStatus.PROCESSING)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingPlatformFees = pendingTransactionVolume.multiply(PLATFORM_FEE_PERCENTAGE);
        
        // Collected platform fees are from completed transactions
        BigDecimal collectedPlatformFees = totalPlatformFees;

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .usersWithFaceEnrolled(usersWithFaceEnrolled)
                .usersWithCardLinked(usersWithCardLinked)
                .totalMerchants(totalMerchants)
                .activeMerchants(activeMerchants)
                .pendingMerchants(pendingMerchants)
                .suspendedMerchants(suspendedMerchants)
                .totalTransactions(totalTransactions)
                .completedTransactions(completedTransactions)
                .pendingTransactions(pendingTransactions)
                .failedTransactions(failedTransactions)
                .totalTransactionVolume(totalTransactionVolume)
                .totalPlatformFees(totalPlatformFees)
                .pendingPlatformFees(pendingPlatformFees)
                .collectedPlatformFees(collectedPlatformFees)
                .transactionsToday(transactionsToday)
                .transactionsThisWeek(transactionsThisWeek)
                .transactionsThisMonth(transactionsThisMonth)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .newMerchantsToday(newMerchantsToday)
                .newMerchantsThisWeek(newMerchantsThisWeek)
                .newMerchantsThisMonth(newMerchantsThisMonth)
                .build();
    }

    public List<UserResponse> getAllUsers() {
        log.info("Admin fetching all users");
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> modelMapper.map(user, UserResponse.class))
                .collect(Collectors.toList());
    }

    public List<MerchantResponse> getAllMerchants() {
        log.info("Admin fetching all merchants");
        List<Merchant> merchants = merchantRepository.findAll();
        return merchants.stream()
                .map(merchant -> modelMapper.map(merchant, MerchantResponse.class))
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getAllTransactions() {
        log.info("Admin fetching all transactions");
        List<Transaction> transactions = transactionRepository.findAll();
        return transactions.stream()
                .map(transaction -> modelMapper.map(transaction, TransactionResponse.class))
                .collect(Collectors.toList());
    }

    private void handleFailedLogin(Admin admin) {
        int attempts = admin.getFailedLoginAttempts() + 1;
        admin.setFailedLoginAttempts(attempts);

        if (attempts >= 5) {
            admin.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            log.warn("Admin account locked due to failed login attempts: {}", admin.getEmail());
        }

        adminRepository.save(admin);
    }
}
