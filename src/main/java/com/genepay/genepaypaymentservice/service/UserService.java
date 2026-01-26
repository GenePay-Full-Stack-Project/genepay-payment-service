package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.exception.BadRequestException;
import com.genepay.genepaypaymentservice.exception.UnauthorizedException;
import com.genepay.genepaypaymentservice.model.User;
import com.genepay.genepaypaymentservice.repository.UserRepository;
import com.genepay.genepaypaymentservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor

public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final EmailService emailService;


    private final java.util.Map<String, String> tempVerificationCodes = new ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> tempExpiry = new ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> verifiedEmails = new ConcurrentHashMap<>();

    public void sendVerificationCode(String email) {
        log.info("Sending verification code to: {}", email);

        String normalizedEmail = email.toLowerCase();

        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        String verificationCode = generateVerificationCode();
        log.info("Verification code: {}", verificationCode);
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);

        tempVerificationCodes.put(normalizedEmail, verificationCode);
        tempExpiry.put(normalizedEmail, expiryTime);

        // Send verification email
        try {
            emailService.sendVerificationEmail(email, "User", verificationCode);
            log.info("Verification email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw new BadRequestException("Failed to send verification email");
        }
    }
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already registered");
        }

        if (userRepository.existsByNicNumber(request.getNicNumber())) {
            throw new BadRequestException("NIC number already registered");
        }

        // Verify the email is verified
        String normalizedEmail = request.getEmail().toLowerCase();
        LocalDateTime verifiedTime = verifiedEmails.get(normalizedEmail);
        if (verifiedTime == null || verifiedTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Email not verified or verification expired");
        }

        // Remove the verified status
        verifiedEmails.remove(normalizedEmail);

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .nicNumber(request.getNicNumber())
                .phoneNumber(request.getPhoneNumber())
                .emailVerified(true) // Since verified
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
            // Don't fail registration if email fails
        }

        return modelMapper.map(user, UserResponse.class);
    }
    @Transactional
    public LoginResponse loginUser(UserLoginRequest request) {
        log.info("User login attempt: {}", request.getNicNumber());

        User user = userRepository.findByNicNumber(request.getNicNumber())
                .orElseThrow(() -> new UnauthorizedException("Invalid NIC or password"));

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException("Account is locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid NIC or password");
        }

        // Check user status
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }

        // Reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String token = jwtUtil.generateToken(user.getEmail(), "USER", user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), "USER", user.getId());

        log.info("User logged in successfully: {}", user.getId());

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L) // 24 hours
                .user(modelMapper.map(user, UserResponse.class))
                .build();
    }
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        log.info("Email verification request for: {}", request.getEmail());

        String normalizedEmail = request.getEmail().toLowerCase();

        String storedCode = tempVerificationCodes.get(normalizedEmail);
        LocalDateTime expiry = tempExpiry.get(normalizedEmail);

        if (storedCode == null || !storedCode.equals(request.getVerificationCode()) || expiry == null || expiry.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired verification code");
        }

        // Mark email as verified
        verifiedEmails.put(normalizedEmail, LocalDateTime.now().plusHours(24));

        // Remove temp code
        tempVerificationCodes.remove(normalizedEmail);
        tempExpiry.remove(normalizedEmail);

        log.info("Email verified successfully for: {}", request.getEmail());
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            log.warn("User account locked due to failed login attempts: {}", user.getEmail());
        }

        userRepository.save(user);
    }

    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }



}