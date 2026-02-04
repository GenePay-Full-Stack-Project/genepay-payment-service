package com.genepay.genepaypaymentservice.repository;

import com.genepay.genepaypaymentservice.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    // User card methods
    List<Card> findByUserIdAndIsActiveTrue(Long userId);

    List<Card> findByUserId(Long userId);

    Optional<Card> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<Card> findByIdAndUserId(Long cardId, Long userId);

    boolean existsByPaymentTokenAndUserId(String paymentToken, Long userId);

    long countByUserIdAndIsActiveTrue(Long userId);

    // Find user's default card (required for refunds)
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.isDefault = true")
    Optional<Card> findDefaultCardByUserId(@Param("userId") Long userId);

    // Merchant card methods
    List<Card> findByMerchantIdAndIsActiveTrue(Long merchantId);

    List<Card> findByMerchantId(Long merchantId);

    Optional<Card> findByMerchantIdAndIsDefaultTrue(Long merchantId);

    Optional<Card> findByIdAndMerchantId(Long cardId, Long merchantId);

    boolean existsByPaymentTokenAndMerchantId(String paymentToken, Long merchantId);

    long countByMerchantIdAndIsActiveTrue(Long merchantId);

    boolean existsByPaymentToken(String paymentToken);

    // Find merchant's default card (required for refunds)
    @Query("SELECT c FROM Card c WHERE c.merchant.id = :merchantId AND c.isDefault = true")
    Optional<Card> findDefaultCardByMerchantId(@Param("merchantId") Long merchantId);

    // Common methods
    Optional<Card> findByPaymentToken(String paymentToken);
}
