package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.*;
import com.genepay.genepaypaymentservice.exception.BadRequestException;
import com.genepay.genepaypaymentservice.exception.ResourceNotFoundException;
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
    private final BankingServiceClient bankingServiceClient;
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
    public UserResponse linkFace(Long userId, LinkFaceRequest request) {
        log.info("Linking face for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByFaceId(request.getFaceId())) {
            throw new BadRequestException("Face ID already registered");
        }

        // Call biometric service to update face document with user_id
        try {
            Boolean success = biometricServiceClient.updateFaceUser(userId, request.getFaceId());
            if (!success) {
                throw new BadRequestException("Biometric service failed to link face");
            }
        } catch (Exception e) {
            log.error("Failed to update face in biometric service: {}", e.getMessage());
            throw new BadRequestException("Failed to link face: " + e.getMessage());
        }

        user.setFaceId(request.getFaceId());
        user.setFaceEnrolled(true);
        userRepository.save(user);

        log.info("Face linked successfully for user: {}", userId);
        return modelMapper.map(user, UserResponse.class);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return modelMapper.map(user, UserResponse.class);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return modelMapper.map(user, UserResponse.class);
    }

    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> modelMapper.map(user, UserResponse.class))
                .collect(Collectors.toList());
    }

    public User findByFaceId(String faceId) {
        return userRepository.findByFaceId(faceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for face ID"));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        log.info("Updating user profile: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already registered");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Require re-verification for new email
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update phone number if provided and different
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BadRequestException("Phone number already registered");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // Update full name if provided
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        user = userRepository.save(user);
        log.info("User profile updated successfully: {}", userId);

        return modelMapper.map(user, UserResponse.class);
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

    public TokenVerifyResponse verifyToken(String token) {
        log.info("Verifying token");

        try {
            String email = jwtUtil.extractEmail(token);
            Long userId = jwtUtil.extractUserId(token);
            String userType = jwtUtil.extractUserType(token);

            // Check if user exists
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
                    .userId(userId)
                    .userType(userType)
                    .expiresAt(expiresAt)
                    .build();

        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            return TokenVerifyResponse.builder()
                    .valid(false)
                    .build();
        }
    }

    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");

        try {
            String email = jwtUtil.extractEmail(refreshToken);
            Long userId = jwtUtil.extractUserId(refreshToken);
            String userType = jwtUtil.extractUserType(refreshToken);

            // Check if user exists
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Validate refresh token
            Boolean isValid = jwtUtil.validateToken(refreshToken, email);

            if (!isValid) {
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

            // Check user status
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                throw new UnauthorizedException("Account is not active");
            }

            // Generate new tokens
            String newToken = jwtUtil.generateToken(email, userType, userId);
            String newRefreshToken = jwtUtil.generateRefreshToken(email, userType, userId);

            log.info("Token refreshed successfully for user: {}", userId);

            return RefreshTokenResponse.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400000L) // 24 hours
                    .build();

        } catch (ResourceNotFoundException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }

    @Transactional
    public UserResponse deleteFace(Long userId) {
        log.info("Deleting face for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getFaceEnrolled() || user.getFaceId() == null) {
            throw new BadRequestException("No face enrolled for this user");
        }

        // Call biometric service to delete face data
        try {
            Boolean success = biometricServiceClient.deleteFace(userId);
            if (!success) {
                throw new BadRequestException("Biometric service failed to delete face");
            }
        } catch (Exception e) {
            log.error("Failed to delete face in biometric service: {}", e.getMessage());
            throw new BadRequestException("Failed to delete face: " + e.getMessage());
        }

        // Update user record
        user.setFaceId(null);
        user.setFaceEnrolled(false);
        userRepository.save(user);

        log.info("Face deleted successfully for user: {}", userId);
        return modelMapper.map(user, UserResponse.class);
    }

}