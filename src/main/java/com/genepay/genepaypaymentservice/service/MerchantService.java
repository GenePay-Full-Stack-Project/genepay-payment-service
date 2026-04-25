package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.exception.BadRequestException;
import com.genepay.genepaypaymentservice.exception.ResourceNotFoundException;
import com.genepay.genepaypaymentservice.exception.UnauthorizedException;
import com.genepay.genepaypaymentservice.model.Merchant;
import com.genepay.genepaypaymentservice.model.VerificationCode;
import com.genepay.genepaypaymentservice.repository.MerchantRepository;
import com.genepay.genepaypaymentservice.repository.VerificationCodeRepository;
import com.genepay.genepaypaymentservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final EmailService emailService;
    private final GoogleAuthService googleAuthService;
    private final VerificationCodeRepository verificationCodeRepository;

    @Transactional
    public void sendVerificationCode(String email) {
        log.info("Sending verification code to merchant: {}", email);

        String normalizedEmail = email.toLowerCase();

        // Check if merchant already exists
        if (merchantRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        // Rate limiting: Check if code was sent recently
        Optional<VerificationCode> existingCode = verificationCodeRepository
                .findByEmailAndType(normalizedEmail, VerificationCode.VerificationType.MERCHANT);
        
        if (existingCode.isPresent() && existingCode.get().getLastCodeSentTime() != null 
                && existingCode.get().getLastCodeSentTime().isAfter(LocalDateTime.now().minusMinutes(1))) {
            throw new BadRequestException("Please wait before requesting another verification code");
        }

        String code = generateVerificationCode();
        log.info("Verification code: {}", code);
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);

        // Delete any existing verification code for this email
        verificationCodeRepository.deleteByEmailAndType(normalizedEmail, VerificationCode.VerificationType.MERCHANT);

        // Create new verification code
        VerificationCode verificationCode = VerificationCode.builder()
                .email(normalizedEmail)
                .code(code)
                .type(VerificationCode.VerificationType.MERCHANT)
                .expiryTime(expiryTime)
                .verified(false)
                .lastCodeSentTime(LocalDateTime.now())
                .build();
        
        verificationCodeRepository.save(verificationCode);

        // Send verification email
        try {
            emailService.sendVerificationEmail(email, "Merchant", code);
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

        VerificationCode verificationCode = verificationCodeRepository
                .findByEmailAndType(normalizedEmail, VerificationCode.VerificationType.MERCHANT)
                .orElseThrow(() -> new BadRequestException("No verification code found for this email"));

        if (!verificationCode.getCode().equals(request.getVerificationCode())) {
            throw new BadRequestException("Invalid verification code");
        }

        if (verificationCode.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification code expired");
        }

        // Mark email as verified
        verificationCode.setVerified(true);
        verificationCode.setVerifiedAt(LocalDateTime.now());
        // Extend expiry for registration completion
        verificationCode.setExpiryTime(LocalDateTime.now().plusHours(24));
        verificationCodeRepository.save(verificationCode);

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
        VerificationCode verificationCode = verificationCodeRepository
                .findByEmailAndType(normalizedEmail, VerificationCode.VerificationType.MERCHANT)
                .orElseThrow(() -> new BadRequestException("Email not verified"));
        
        if (!verificationCode.getVerified() || verificationCode.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Email not verified or verification expired");
        }

        // Remove the verification code
        verificationCodeRepository.delete(verificationCode);

        // Create merchant
        Merchant merchant = Merchant.builder()
                .email(normalizedEmail)
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

        Merchant merchant = merchantRepository.findByEmail(request.getEmail().toLowerCase())
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

    public MerchantResponse getMerchantById(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        return modelMapper.map(merchant, MerchantResponse.class);
    }

    public Merchant getMerchantEntityById(Long merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
    }

    @Transactional
    public MerchantResponse updateMerchant(Long merchantId, UpdateMerchantRequest request) {
        log.info("Updating merchant profile: {}", merchantId);

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(merchant.getEmail())) {
            if (merchantRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already registered");
            }
            merchant.setEmail(request.getEmail());
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            merchant.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update phone number if provided and different
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(merchant.getPhoneNumber())) {
            if (merchantRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BadRequestException("Phone number already registered");
            }
            merchant.setPhoneNumber(request.getPhoneNumber());
        }

        // Update business name if provided and different
        if (request.getBusinessName() != null && !request.getBusinessName().equals(merchant.getBusinessName())) {
            if (merchantRepository.existsByBusinessName(request.getBusinessName())) {
                throw new BadRequestException("Business name already registered");
            }
            merchant.setBusinessName(request.getBusinessName());
        }

        // Update other fields if provided
        if (request.getOwnerName() != null) {
            merchant.setOwnerName(request.getOwnerName());
        }

        if (request.getBusinessAddress() != null) {
            merchant.setBusinessAddress(request.getBusinessAddress());
        }

        if (request.getBusinessType() != null) {
            merchant.setBusinessType(request.getBusinessType());
        }

        merchant = merchantRepository.save(merchant);
        log.info("Merchant profile updated successfully: {}", merchantId);

        return modelMapper.map(merchant, MerchantResponse.class);
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

    public TokenVerifyResponse verifyToken(String token) {
        log.info("Verifying merchant token");

        try {
            String email = jwtUtil.extractEmail(token);
            Long merchantId = jwtUtil.extractUserId(token);
            String userType = jwtUtil.extractUserType(token);

            // Check if merchant exists
            merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));

            // Validate token
            Boolean isValid = jwtUtil.validateToken(token, email);

            if (!isValid) {
                return TokenVerifyResponse.builder()
                        .valid(false)
                        .build();
            }

            Long expiresAt = jwtUtil.extractExpiration(token).getTime();

            return TokenVerifyResponse.builder()
                    .valid(true)
                    .email(email)
                    .userId(merchantId)
                    .userType(userType)
                    .expiresAt(expiresAt)
                    .build();

        } catch (Exception e) {
            log.error("Merchant token verification failed: {}", e.getMessage());
            return TokenVerifyResponse.builder()
                    .valid(false)
                    .build();
        }
    }

    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken) {
        log.info("Refreshing merchant token");

        try {
            String email = jwtUtil.extractEmail(refreshToken);
            Long merchantId = jwtUtil.extractUserId(refreshToken);
            String userType = jwtUtil.extractUserType(refreshToken);

            // Check if merchant exists
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));

            // Validate refresh token
            Boolean isValid = jwtUtil.validateToken(refreshToken, email);

            if (!isValid) {
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

            // Check merchant status
            if (merchant.getStatus() != Merchant.MerchantStatus.ACTIVE &&
                    merchant.getStatus() != Merchant.MerchantStatus.PENDING) {
                throw new UnauthorizedException("Account is not active");
            }

            // Generate new tokens
            String newToken = jwtUtil.generateToken(email, userType, merchantId);
            String newRefreshToken = jwtUtil.generateRefreshToken(email, userType, merchantId);

            log.info("Token refreshed successfully for merchant: {}", merchantId);

            return RefreshTokenResponse.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400000L) // 24 hours
                    .build();

        } catch (ResourceNotFoundException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Merchant token refresh failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }

    /**
     * Sign in or register merchant with Google
     * @param request GoogleSignInRequest containing Google ID token
     * @return LoginResponse with JWT tokens
     */
    @Transactional
    public LoginResponse googleSignIn(GoogleSignInRequest request) {
        log.info("Google sign-in request received for merchant");

        // Verify Google token and get user info
        GoogleUserInfo googleUserInfo = googleAuthService.verifyGoogleToken(request.getIdToken());

        if (!googleUserInfo.getEmailVerified()) {
            throw new BadRequestException("Google email not verified");
        }

        // Check if merchant exists by googleId
        Merchant merchant = merchantRepository.findByGoogleId(googleUserInfo.getGoogleId())
                .orElse(null);

        // If not found by googleId, check by email
        if (merchant == null) {
            merchant = merchantRepository.findByEmail(googleUserInfo.getEmail())
                    .orElse(null);

            if (merchant != null) {
                // Merchant exists with this email but not linked to Google yet
                // Link the Google account
                merchant.setGoogleId(googleUserInfo.getGoogleId());
                merchant = merchantRepository.save(merchant);
                log.info("Linked existing merchant account to Google: {}", merchant.getEmail());
            }
        }

        // If merchant still doesn't exist, create new merchant
        if (merchant == null) {
            log.info("Creating new merchant from Google sign-in: {}", googleUserInfo.getEmail());
            
            merchant = Merchant.builder()
                    .email(googleUserInfo.getEmail())
                    .businessName(googleUserInfo.getName())
                    .ownerName(googleUserInfo.getName())
                    .googleId(googleUserInfo.getGoogleId())
                    .status(Merchant.MerchantStatus.PENDING)
                    .build();

            merchant = merchantRepository.save(merchant);
            log.info("New merchant created from Google sign-in: {}", merchant.getId());

            // Send welcome email
            try {
                emailService.sendWelcomeEmail(merchant.getEmail(), merchant.getBusinessName());
                log.info("Welcome email sent to: {}", merchant.getEmail());
            } catch (Exception e) {
                log.error("Failed to send welcome email to: {}", merchant.getEmail(), e);
                // Don't fail if email fails
            }
        } else {
            // Update last login time
            merchant.setLastLoginAt(LocalDateTime.now());
            merchant.setFailedLoginAttempts(0);
            merchant.setLockedUntil(null);
            merchantRepository.save(merchant);
        }

        // Check merchant status
        if (merchant.getStatus() != Merchant.MerchantStatus.ACTIVE &&
                merchant.getStatus() != Merchant.MerchantStatus.PENDING) {
            throw new UnauthorizedException("Account is not active");
        }

        // Generate tokens
        String token = jwtUtil.generateToken(merchant.getEmail(), "MERCHANT", merchant.getId());
        String refreshToken = jwtUtil.generateRefreshToken(merchant.getEmail(), "MERCHANT", merchant.getId());

        log.info("Google sign-in successful for merchant: {}", merchant.getId());

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
}