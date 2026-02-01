package com.genepay.genepaypaymentservice.repository;

import com.genepay.genepaypaymentservice.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserIdAndIsActiveTrue(Long userId);

    List<Card> findByMerchantIdAndIsActiveTrue(Long merchantId);

    Optional<Card> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<Card> findByMerchantIdAndIsDefaultTrue(Long merchantId);

    Optional<Card> findByPaymentToken(String paymentToken);

    Optional<Card> findByIdAndUserId(Long cardId, Long userId);

    Optional<Card> findByIdAndMerchantId(Long cardId, Long merchantId);

    boolean existsByPaymentToken(String paymentToken);

    boolean existsByPaymentTokenAndMerchantId(String paymentToken, Long merchantId);

    boolean existsByPaymentTokenAndUserId(String paymentToken, Long userId);

    long countByUserIdAndIsActiveTrue(Long userId);

    long countByMerchantIdAndIsActiveTrue(Long merchantId);
}
