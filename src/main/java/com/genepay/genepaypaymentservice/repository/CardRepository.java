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

    // Find user's default card (required for refunds)
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.isDefault = true")
    Optional<Card> findDefaultCardByUserId(@Param("userId") Long userId);

    // Find merchant's default card (required for refunds)
    @Query("SELECT c FROM Card c WHERE c.merchant.id = :merchantId AND c.isDefault = true")
    Optional<Card> findDefaultCardByMerchantId(@Param("merchantId") Long merchantId);

    List<Card> findByUserId(Long userId);

    List<Card> findByMerchantId(Long merchantId);

    Optional<Card> findByPaymentToken(String paymentToken);

    Optional<Card> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<Card> findByMerchantIdAndIsDefaultTrue(Long merchantId);

}