package com.genepay.genepaypaymentservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false, unique = true)
    private String paymentToken; // Banking System payment token (UUID)

    @Column(nullable = false)
    private String cardLast4; // Last 4 digits of card

    @Column
    private String cardBrand; // Visa, Mastercard, etc.

    @Column
    private String expiryMonth;

    @Column
    private String expiryYear;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column
    private String nickname; // Optional card nickname (e.g., "Personal Card", "Business Card")

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastUsedAt;

    // Helper method to check if card belongs to user
    public boolean belongsToUser() {
        return user != null;
    }

    // Helper method to check if card belongs to merchant
    public boolean belongsToMerchant() {
        return merchant != null;
    }
}
