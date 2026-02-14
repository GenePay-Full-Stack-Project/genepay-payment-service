package com.genepay.genepaypaymentservice.controller;

import com.genepay.genepaypaymentservice.dto.AddCardRequest;
import com.genepay.genepaypaymentservice.dto.ApiResponse;
import com.genepay.genepaypaymentservice.dto.CardResponse;
import com.genepay.genepaypaymentservice.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management", description = "APIs for managing user and merchant payment cards")
public class CardController {

    private final CardService cardService;

    // ========== USER CARD ENDPOINTS ==========

    @PostMapping("/user/{userId}")
    @Operation(summary = "Add User Card", description = "Link a new payment card to user account")
    public ResponseEntity<ApiResponse<CardResponse>> addUserCard(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody AddCardRequest request) {
        log.info("Add card request for user: {}", userId);
        CardResponse response = cardService.addUserCard(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Card added successfully", response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Cards", description = "Get all active cards for a user")
    public ResponseEntity<ApiResponse<List<CardResponse>>> getUserCards(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("Get cards for user: {}", userId);
        List<CardResponse> cards = cardService.getUserCards(userId);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/user/{userId}/default")
    @Operation(summary = "Get User Default Card", description = "Get the default payment card for a user")
    public ResponseEntity<ApiResponse<CardResponse>> getUserDefaultCard(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("Get default card for user: {}", userId);
        CardResponse card = cardService.getUserDefaultCard(userId);
        return ResponseEntity.ok(ApiResponse.success(card));
    }

    @PutMapping("/user/{userId}/{cardId}/set-default")
    @Operation(summary = "Set User Default Card", description = "Set a specific card as the default for payments")
    public ResponseEntity<ApiResponse<CardResponse>> setUserDefaultCard(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Card ID") @PathVariable Long cardId) {
        log.info("Set default card {} for user: {}", cardId, userId);
        CardResponse response = cardService.setUserDefaultCard(userId, cardId);
        return ResponseEntity.ok(ApiResponse.success("Default card updated", response));
    }

    @DeleteMapping("/user/{userId}/{cardId}")
    @Operation(summary = "Remove User Card", description = "Remove a payment card from user account")
    public ResponseEntity<ApiResponse<Void>> removeUserCard(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Card ID") @PathVariable Long cardId) {
        log.info("Remove card {} for user: {}", cardId, userId);
        cardService.removeUserCard(userId, cardId);
        return ResponseEntity.ok(ApiResponse.success("Card removed successfully", null));
    }

    @PutMapping("/user/{userId}/{cardId}/nickname")
    @Operation(summary = "Update Card Nickname", description = "Update the nickname for a user's card")
    public ResponseEntity<ApiResponse<CardResponse>> updateUserCardNickname(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Card ID") @PathVariable Long cardId,
            @RequestBody Map<String, String> request) {
        log.info("Update card nickname for user: {}", userId);
        CardResponse response = cardService.updateCardNickname(userId, cardId, request.get("nickname"));
        return ResponseEntity.ok(ApiResponse.success("Card nickname updated", response));
    }

    // ========== MERCHANT CARD ENDPOINTS ==========

    @PostMapping("/merchant/{merchantId}")
    @Operation(summary = "Add Merchant Card", description = "Link a new payment card to merchant account")
    public ResponseEntity<ApiResponse<CardResponse>> addMerchantCard(
            @Parameter(description = "Merchant ID") @PathVariable Long merchantId,
            @Valid @RequestBody AddCardRequest request) {
        log.info("Add card request for merchant: {}", merchantId);
        CardResponse response = cardService.addMerchantCard(merchantId, request);
        return ResponseEntity.ok(ApiResponse.success("Card added successfully", response));
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get Merchant Cards", description = "Get all active cards for a merchant")
    public ResponseEntity<ApiResponse<List<CardResponse>>> getMerchantCards(
            @Parameter(description = "Merchant ID") @PathVariable Long merchantId) {
        log.info("Get cards for merchant: {}", merchantId);
        List<CardResponse> cards = cardService.getMerchantCards(merchantId);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/merchant/{merchantId}/default")
    @Operation(summary = "Get Merchant Default Card", description = "Get the default payment card for a merchant")
    public ResponseEntity<ApiResponse<CardResponse>> getMerchantDefaultCard(
            @Parameter(description = "Merchant ID") @PathVariable Long merchantId) {
        log.info("Get default card for merchant: {}", merchantId);
        CardResponse card = cardService.getMerchantDefaultCard(merchantId);
        return ResponseEntity.ok(ApiResponse.success(card));
    }

    @PutMapping("/merchant/{merchantId}/{cardId}/set-default")
    @Operation(summary = "Set Merchant Default Card", description = "Set a specific card as the default for receiving payments")
    public ResponseEntity<ApiResponse<CardResponse>> setMerchantDefaultCard(
            @Parameter(description = "Merchant ID") @PathVariable Long merchantId,
            @Parameter(description = "Card ID") @PathVariable Long cardId) {
        log.info("Set default card {} for merchant: {}", cardId, merchantId);
        CardResponse response = cardService.setMerchantDefaultCard(merchantId, cardId);
        return ResponseEntity.ok(ApiResponse.success("Default card updated", response));
    }

    @DeleteMapping("/merchant/{merchantId}/{cardId}")
    @Operation(summary = "Remove Merchant Card", description = "Remove a payment card from merchant account")
    public ResponseEntity<ApiResponse<Void>> removeMerchantCard(
            @Parameter(description = "Merchant ID") @PathVariable Long merchantId,
            @Parameter(description = "Card ID") @PathVariable Long cardId) {
        log.info("Remove card {} for merchant: {}", cardId, merchantId);
        cardService.removeMerchantCard(merchantId, cardId);
        return ResponseEntity.ok(ApiResponse.success("Card removed successfully", null));
    }
}
