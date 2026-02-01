package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.CardResponse;
import com.genepay.genepaypaymentservice.exception.BadRequestException;
import com.genepay.genepaypaymentservice.exception.ResourceNotFoundException;
import com.genepay.genepaypaymentservice.model.Card;
import com.genepay.genepaypaymentservice.repository.CardRepository;
import com.genepay.genepaypaymentservice.repository.MerchantRepository;
import com.genepay.genepaypaymentservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final BankingServiceClient bankingServiceClient;

    @Transactional
    public CardResponse addUserCard(Long userId, AddCardRequest request) {
        log.info("Adding card for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Call Banking System API to verify card and get payment token
        String paymentToken = bankingServiceClient.verifyCardAndGetToken(
                request.getCardNumber(),
                request.getCvv(),
                request.getExpiry()
        );

        if (paymentToken == null) {
            throw new BadRequestException("Invalid card details or card verification failed");
        }

        // Check if this user already has this card (even if previously removed)
        if (cardRepository.existsByPaymentTokenAndUserId(paymentToken, userId)) {
            throw new BadRequestException("This card is already linked to your account");
        }

        // Check if card is linked to another account
        if (cardRepository.existsByPaymentToken(paymentToken)) {
            throw new BadRequestException("This card is already linked to another account");
        }

        String cardLast4 = request.getCardNumber().substring(12);
        String[] expiryParts = request.getExpiry().split("/");

        // If this is first card or set as default, unset other defaults
        if (request.getSetAsDefault() || cardRepository.countByUserIdAndIsActiveTrue(userId) == 0) {
            unsetUserDefaultCards(userId);
        }

        Card card = Card.builder()
                .user(user)
                .paymentToken(paymentToken)
                .cardLast4(cardLast4)
                .expiryMonth(expiryParts[0])
                .expiryYear("20" + expiryParts[1])
                .nickname(request.getNickname())
                .isDefault(request.getSetAsDefault() || cardRepository.countByUserIdAndIsActiveTrue(userId) == 0)
                .isActive(true)
                .build();

        card = cardRepository.save(card);

        // Update user's cardLinked status
        if (!user.getCardLinked()) {
            user.setCardLinked(true);
            userRepository.save(user);
        }

        log.info("Card added successfully for user: {} (Last 4: {})", userId, cardLast4);
        return mapToCardResponse(card);
    }

    @Transactional
    public CardResponse addMerchantCard(Long merchantId, AddCardRequest request) {
        log.info("Adding card for merchant: {}", merchantId);

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));

        // Call Banking System API to verify card and get payment token
        String paymentToken = bankingServiceClient.verifyCardAndGetToken(
                request.getCardNumber(),
                request.getCvv(),
                request.getExpiry()
        );

        if (paymentToken == null) {
            throw new BadRequestException("Invalid card details or card verification failed");
        }

        // Check if this merchant already has this card (even if previously removed)
        if (cardRepository.existsByPaymentTokenAndMerchantId(paymentToken, merchantId)) {
            throw new BadRequestException("This card is already linked to your account");
        }

        // Check if card is linked to another account
        if (cardRepository.existsByPaymentToken(paymentToken)) {
            throw new BadRequestException("This card is already linked to another account");
        }

        String cardLast4 = request.getCardNumber().substring(12);
        String[] expiryParts = request.getExpiry().split("/");

        // If this is first card or set as default, unset other defaults
        if (request.getSetAsDefault() || cardRepository.countByMerchantIdAndIsActiveTrue(merchantId) == 0) {
            unsetMerchantDefaultCards(merchantId);
        }

        Card card = Card.builder()
                .merchant(merchant)
                .paymentToken(paymentToken)
                .cardLast4(cardLast4)
                .expiryMonth(expiryParts[0])
                .expiryYear("20" + expiryParts[1])
                .nickname(request.getNickname())
                .isDefault(request.getSetAsDefault() || cardRepository.countByMerchantIdAndIsActiveTrue(merchantId) == 0)
                .isActive(true)
                .build();

        card = cardRepository.save(card);

        // Update merchant's cardLinked status
        if (!merchant.getCardLinked()) {
            merchant.setCardLinked(true);
            merchantRepository.save(merchant);
        }

        log.info("Card added successfully for merchant: {} (Last 4: {})", merchantId, cardLast4);
        return mapToCardResponse(card);
    }

    public List<CardResponse> getUserCards(Long userId) {
        return cardRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::mapToCardResponse)
                .collect(Collectors.toList());
    }

    public List<CardResponse> getMerchantCards(Long merchantId) {
        return cardRepository.findByMerchantIdAndIsActiveTrue(merchantId).stream()
                .map(this::mapToCardResponse)
                .collect(Collectors.toList());
    }

    public CardResponse getUserDefaultCard(Long userId) {
        Card card = cardRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No default card found"));
        return mapToCardResponse(card);
    }

    public CardResponse getMerchantDefaultCard(Long merchantId) {
        Card card = cardRepository.findByMerchantIdAndIsDefaultTrue(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("No default card found"));
        return mapToCardResponse(card);
    }

    @Transactional
    public CardResponse setUserDefaultCard(Long userId, Long cardId) {
        log.info("Setting default card {} for user: {}", cardId, userId);

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!card.getIsActive()) {
            throw new BadRequestException("Cannot set inactive card as default");
        }

        // Unset current default
        unsetUserDefaultCards(userId);

        // Set new default
        card.setIsDefault(true);
        card = cardRepository.save(card);

        log.info("Default card updated for user: {}", userId);
        return mapToCardResponse(card);
    }

    @Transactional
    public CardResponse setMerchantDefaultCard(Long merchantId, Long cardId) {
        log.info("Setting default card {} for merchant: {}", cardId, merchantId);

        Card card = cardRepository.findByIdAndMerchantId(cardId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!card.getIsActive()) {
            throw new BadRequestException("Cannot set inactive card as default");
        }

        // Unset current default
        unsetMerchantDefaultCards(merchantId);

        // Set new default
        card.setIsDefault(true);
        card = cardRepository.save(card);

        log.info("Default card updated for merchant: {}", merchantId);
        return mapToCardResponse(card);
    }

    @Transactional
    public void removeUserCard(Long userId, Long cardId) {
        log.info("Removing card {} for user: {}", cardId, userId);

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        boolean wasDefault = card.getIsDefault();

        // Soft delete
        card.setIsActive(false);
        card.setIsDefault(false);
        cardRepository.save(card);

        // If it was default, set another card as default
        if (wasDefault) {
            List<Card> remainingCards = cardRepository.findByUserIdAndIsActiveTrue(userId);
            if (!remainingCards.isEmpty()) {
                Card newDefault = remainingCards.get(0);
                newDefault.setIsDefault(true);
                cardRepository.save(newDefault);
            } else {
                // No cards left, update user
                User user = userRepository.findById(userId).orElseThrow();
                user.setCardLinked(false);
                userRepository.save(user);
            }
        }

        log.info("Card removed for user: {}", userId);
    }

    @Transactional
    public void removeMerchantCard(Long merchantId, Long cardId) {
        log.info("Removing card {} for merchant: {}", cardId, merchantId);

        Card card = cardRepository.findByIdAndMerchantId(cardId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        boolean wasDefault = card.getIsDefault();

        // Soft delete
        card.setIsActive(false);
        card.setIsDefault(false);
        cardRepository.save(card);

        // If it was default, set another card as default
        if (wasDefault) {
            List<Card> remainingCards = cardRepository.findByMerchantIdAndIsActiveTrue(merchantId);
            if (!remainingCards.isEmpty()) {
                Card newDefault = remainingCards.get(0);
                newDefault.setIsDefault(true);
                cardRepository.save(newDefault);
            } else {
                // No cards left, update merchant
                Merchant merchant = merchantRepository.findById(merchantId).orElseThrow();
                merchant.setCardLinked(false);
                merchantRepository.save(merchant);
            }
        }

        log.info("Card removed for merchant: {}", merchantId);
    }

    @Transactional
    public CardResponse updateCardNickname(Long userId, Long cardId, String nickname) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        card.setNickname(nickname);
        card = cardRepository.save(card);

        return mapToCardResponse(card);
    }

    private void unsetUserDefaultCards(Long userId) {
        cardRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(card -> {
            card.setIsDefault(false);
            cardRepository.save(card);
        });
    }

    private void unsetMerchantDefaultCards(Long merchantId) {
        cardRepository.findByMerchantIdAndIsDefaultTrue(merchantId).ifPresent(card -> {
            card.setIsDefault(false);
            cardRepository.save(card);
        });
    }

    public String getUserDefaultPaymentToken(Long userId) {
        Card card = cardRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No default card found for user"));
        return card.getPaymentToken();
    }

    public String getMerchantDefaultPaymentToken(Long merchantId) {
        Card card = cardRepository.findByMerchantIdAndIsDefaultTrue(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("No default card found for merchant"));
        return card.getPaymentToken();
    }
    @Transactional
    public void updateCardLastUsed(String paymentToken) {
        cardRepository.findByPaymentToken(paymentToken).ifPresent(card -> {
            card.setLastUsedAt(LocalDateTime.now());
            cardRepository.save(card);
        });
    }

    private CardResponse mapToCardResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .cardLast4(card.getCardLast4())
                .cardBrand(card.getCardBrand())
                .expiryMonth(card.getExpiryMonth())
                .expiryYear(card.getExpiryYear())
                .isDefault(card.getIsDefault())
                .isActive(card.getIsActive())
                .nickname(card.getNickname())
                .createdAt(card.getCreatedAt())
                .lastUsedAt(card.getLastUsedAt())
                .build();
    }
}
