package com.genepay.genepaypaymentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String nicNumber;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(unique = true)
    private String faceId; // Reference to biometric service

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Card> cards = new java.util.ArrayList<>();

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column
    private String emailVerificationCode;

    @Column
    private LocalDateTime emailVerificationExpiry;

    @Column(nullable = false)
    @Builder.Default
    private Boolean faceEnrolled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean cardLinked = false;

    @Column
    private Integer failedLoginAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    public enum UserStatus {
        ACTIVE, SUSPENDED, INACTIVE, DELETED
    }
}
