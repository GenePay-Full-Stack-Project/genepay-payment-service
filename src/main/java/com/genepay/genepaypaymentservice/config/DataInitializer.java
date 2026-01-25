package com.genepay.genepaypaymentservice.config;

import com.genepay.genepaypaymentservice.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Data initializer to create default admin users on application startup
 * This runs after the database migrations have been applied
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Initializing default admin users...");
        
        createAdminIfNotExists(
            "admin@biopay.com",
            "Admin@123",
            "System",
            "Administrator",
            Admin.AdminRole.SUPER_ADMIN
        );
        
        createAdminIfNotExists(
            "support@biopay.com",
            "Support@123",
            "Support",
            "Team",
            Admin.AdminRole.SUPPORT
        );
        
        createAdminIfNotExists(
            "operator@biopay.com",
            "Operator@123",
            "System",
            "Operator",
            Admin.AdminRole.ADMIN
        );
        
        log.info("Admin users initialization completed");
    }

    private void createAdminIfNotExists(String email, String password, String firstName, 
                                       String lastName, Admin.AdminRole role) {
        if (adminRepository.findByEmail(email).isEmpty()) {
            Admin admin = Admin.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(role)
                    .status(Admin.AdminStatus.ACTIVE)
                    .failedLoginAttempts(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            adminRepository.save(admin);
            log.info("Created admin user: {} with role: {}", email, role);
        } else {
            // Update password if admin exists (useful for password resets during development)
            Admin existingAdmin = adminRepository.findByEmail(email).get();
            String newEncodedPassword = passwordEncoder.encode(password);
            
            // Only update if password has changed
            if (!passwordEncoder.matches(password, existingAdmin.getPassword())) {
                existingAdmin.setPassword(newEncodedPassword);
                existingAdmin.setUpdatedAt(LocalDateTime.now());
                adminRepository.save(existingAdmin);
                log.info("Updated password for admin user: {}", email);
            } else {
                log.info("Admin user already exists: {}", email);
            }
        }
    }
}
