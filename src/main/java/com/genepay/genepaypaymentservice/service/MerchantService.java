package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.exception.BadRequestException;
import com.genepay.genepaypaymentservice.exception.ResourceNotFoundException;
import com.genepay.genepaypaymentservice.exception.UnauthorizedException;
import com.genepay.genepaypaymentservice.model.Merchant;
import com.genepay.genepaypaymentservice.repository.MerchantRepository;
import com.genepay.genepaypaymentservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    private final java.util.Map<String, String> tempVerificationCodes = new ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> tempExpiry = new ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> verifiedEmails = new ConcurrentHashMap<>();
    // Rate limiting for verification code sending
    private final java.util.Map<String, LocalDateTime> lastCodeSentTime = new ConcurrentHashMap<>();

    public void sendVerificationCode(String email) {
        log.info("Sending verification code to merchant: {}", email);

        String normalizedEmail = email.toLowerCase();

        // Check if merchant already exists
        if (merchantRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        // Rate limiting: Allow sending code only once per minute
        LocalDateTime lastSent = lastCodeSentTime.get(normalizedEmail);
        if (lastSent != null && lastSent.isAfter(LocalDateTime.now().minusMinutes(1))) {
            throw new BadRequestException("Please wait before requesting another verification code");
        }

        String verificationCode = generateVerificationCode();
        log.info("Verification code: {}", verificationCode);
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);

        tempVerificationCodes.put(normalizedEmail, verificationCode);
        tempExpiry.put(normalizedEmail, expiryTime);
        lastCodeSentTime.put(normalizedEmail, LocalDateTime.now());

        // Send verification email
        try {
            emailService.sendVerificationEmail(email, "Merchant", verificationCode);
            log.info("Verification email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw new BadRequestException("Failed to send verification email");
        }
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        log.info("Email verification request for merchant: {}", request.getEmail());

        String normalizedEmail = request.getEmail().toLowerCase();

        String storedCode = tempVerificationCodes.get(normalizedEmail);
        LocalDateTime expiry = tempExpiry.get(normalizedEmail);

        if (storedCode == null || !storedCode.equals(request.getVerificationCode()) ||
                expiry == null || expiry.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired verification code");
        }

        // Mark email as verified
        verifiedEmails.put(normalizedEmail, LocalDateTime.now().plusHours(24));

        // Remove temp code
        tempVerificationCodes.remove(normalizedEmail);
        tempExpiry.remove(normalizedEmail);

        log.info("Email verified successfully for merchant: {}", request.getEmail());
    }

    @Transactional
    public MerchantResponse registerMerchant(MerchantRegistrationRequest request) {
        log.info("Registering new merchant: {}", request.getEmail());

        // Check if merchant already exists
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        if (request.getPhoneNumber() != null && merchantRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already registered");
        }

        if (merchantRepository.existsByBusinessName(request.getBusinessName())) {
            throw new BadRequestException("Business name already registered");
        }

        // Verify the email is verified
        String normalizedEmail = request.getEmail().toLowerCase();
        LocalDateTime verifiedTime = verifiedEmails.get(normalizedEmail);
        if (verifiedTime == null || verifiedTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Email not verified or verification expired");
        }

        // Remove the verified status
        verifiedEmails.remove(normalizedEmail);

        // Create merchant
        Merchant merchant = Merchant.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .businessName(request.getBusinessName())
                .ownerName(request.getOwnerName())
                .phoneNumber(request.getPhoneNumber())
                .businessAddress(request.getBusinessAddress())
                .businessType(request.getBusinessType())
                .status(Merchant.MerchantStatus.PENDING)
                .build();

        merchant = merchantRepository.save(merchant);
        log.info("Merchant registered successfully: {}", merchant.getId());

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(merchant.getEmail(), merchant.getBusinessName());
            log.info("Welcome email sent to: {}", merchant.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", merchant.getEmail(), e);
            // Don't fail registration if email fails
        }

        return modelMapper.map(merchant, MerchantResponse.class);
    }

    @Transactional
    public LoginResponse loginMerchant(LoginRequest request) {
        log.info("Merchant login attempt: {}", request.getEmail());

        Merchant merchant = merchantRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if account is locked
        if (merchant.getLockedUntil() != null && merchant.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException("Account is locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), merchant.getPassword())) {
            handleFailedLogin(merchant);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check merchant status
        if (merchant.getStatus() != Merchant.MerchantStatus.ACTIVE &&
                merchant.getStatus() != Merchant.MerchantStatus.PENDING) {
            throw new UnauthorizedException("Account is not active");
        }

        // Reset failed attempts
        merchant.setFailedLoginAttempts(0);
        merchant.setLockedUntil(null);
        merchant.setLastLoginAt(LocalDateTime.now());
        merchantRepository.save(merchant);

        // Generate tokens
        String token = jwtUtil.generateToken(merchant.getEmail(), "MERCHANT", merchant.getId());
        String refreshToken = jwtUtil.generateRefreshToken(merchant.getEmail(), "MERCHANT", merchant.getId());

        log.info("Merchant logged in successfully: {}", merchant.getId());

        UserResponse merchantAsUser = UserResponse.builder()
                .id(merchant.getId())
                .email(merchant.getEmail())
                .fullName(merchant.getBusinessName() +
                        (merchant.getOwnerName() != null ? " - " + merchant.getOwnerName() : ""))
                .build();

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(merchantAsUser)
                .build();
    }

    private void handleFailedLogin(Merchant merchant) {
        int attempts = merchant.getFailedLoginAttempts() + 1;
        merchant.setFailedLoginAttempts(attempts);

        if (attempts >= 5) {
            merchant.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            log.warn("Merchant account locked due to failed login attempts: {}", merchant.getEmail());
        }

        merchantRepository.save(merchant);
    }

    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}