package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.exception.ResourceNotFoundException;
import com.genepay.genepaypaymentservice.model.Card;
import com.genepay.genepaypaymentservice.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

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
}