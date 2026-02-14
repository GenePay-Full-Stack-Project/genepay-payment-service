package com.genepay.genepaypaymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genepay.genepaypaymentservice.dto.LoginRequest;
import com.genepay.genepaypaymentservice.dto.LoginResponse;
import com.genepay.genepaypaymentservice.dto.MerchantRegistrationRequest;
import com.genepay.genepaypaymentservice.dto.MerchantResponse;
import com.genepay.genepaypaymentservice.dto.RefreshTokenRequest;
import com.genepay.genepaypaymentservice.dto.RefreshTokenResponse;
import com.genepay.genepaypaymentservice.dto.SendVerificationCodeRequest;
import com.genepay.genepaypaymentservice.dto.TokenVerifyResponse;
import com.genepay.genepaypaymentservice.dto.UpdateMerchantRequest;
import com.genepay.genepaypaymentservice.dto.VerifyEmailRequest;
import com.genepay.genepaypaymentservice.dto.VerifyTokenRequest;
import com.genepay.genepaypaymentservice.service.MerchantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MerchantController.class)
@AutoConfigureMockMvc(addFilters = false)
class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MerchantService merchantService;


    // send-verification-code unit testing scripts

    @Test
    void sendVerificationCode_Success() throws Exception {
        // Arrange
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("merchant@example.com");

        // Act & Assert
        mockMvc.perform(post("/merchants/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification code sent to email"));

        // Verify service was called
        verify(merchantService, times(1)).sendVerificationCode("merchant@example.com");
    }

    @Test
    void sendVerificationCode_WithInvalidEmail() throws Exception {
        // Arrange
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/merchants/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).sendVerificationCode(anyString());
    }

    @Test
    void sendVerificationCode_WithEmptyEmail() throws Exception {
        // Arrange
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("");

        // Act & Assert
        mockMvc.perform(post("/merchants/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).sendVerificationCode(anyString());
    }

    @Test
    void sendVerificationCode_WithNullEmail() throws Exception {
        // Arrange
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/merchants/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).sendVerificationCode(anyString());
    }


    // verify-email unit testing scripts

    @Test
    void verifyEmail_Success() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("merchant@example.com");
        request.setVerificationCode("123456");

        // Act & Assert
        mockMvc.perform(post("/merchants/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        // Verify service was called
        verify(merchantService, times(1)).verifyEmail(any(VerifyEmailRequest.class));
    }

    @Test
    void verifyEmail_WithInvalidEmail() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("invalid-email");
        request.setVerificationCode("123456");

        // Act & Assert
        mockMvc.perform(post("/merchants/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).verifyEmail(any(VerifyEmailRequest.class));
    }

    @Test
    void verifyEmail_WithEmptyEmail() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("");
        request.setVerificationCode("123456");

        // Act & Assert
        mockMvc.perform(post("/merchants/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).verifyEmail(any(VerifyEmailRequest.class));
    }

    @Test
    void verifyEmail_WithEmptyVerificationCode() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("merchant@example.com");
        request.setVerificationCode("");

        // Act & Assert
        mockMvc.perform(post("/merchants/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).verifyEmail(any(VerifyEmailRequest.class));
    }


    // register-merchant unit testing scripts

    @Test
    void registerMerchant_Success() throws Exception {
        // Arrange
        MerchantRegistrationRequest request = new MerchantRegistrationRequest();
        request.setEmail("merchant@example.com");
        request.setPassword("Password@123");
        request.setBusinessName("Test Business");
        request.setOwnerName("John Doe");
        request.setPhoneNumber("+94771234567");

        MerchantResponse merchantResponse = new MerchantResponse();
        merchantResponse.setEmail("merchant@example.com");
        merchantResponse.setBusinessName("Test Business");
        merchantResponse.setOwnerName("John Doe");

        when(merchantService.registerMerchant(any(MerchantRegistrationRequest.class))).thenReturn(merchantResponse);

        // Act & Assert
        mockMvc.perform(post("/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Merchant registered successfully"))
                .andExpect(jsonPath("$.data.email").value("merchant@example.com"));

        // Verify service was called
        verify(merchantService, times(1)).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_WithInvalidEmail() throws Exception {
        // Arrange
        MerchantRegistrationRequest request = new MerchantRegistrationRequest();
        request.setEmail("invalid-email");
        request.setPassword("Password@123");
        request.setBusinessName("Test Business");
        request.setOwnerName("John Doe");
        request.setPhoneNumber("+94771234567");

        // Act & Assert
        mockMvc.perform(post("/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_WithEmptyFields() throws Exception {
        // Arrange
        MerchantRegistrationRequest request = new MerchantRegistrationRequest();
        request.setEmail("");
        request.setPassword("");
        request.setBusinessName("");
        request.setOwnerName("");
        request.setPhoneNumber("");

        // Act & Assert
        mockMvc.perform(post("/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_WithNullEmail() throws Exception {
        // Arrange
        MerchantRegistrationRequest request = new MerchantRegistrationRequest();
        request.setEmail(null);
        request.setPassword("Password@123");
        request.setBusinessName("Test Business");
        request.setOwnerName("John Doe");
        request.setPhoneNumber("+94771234567");

        // Act & Assert
        mockMvc.perform(post("/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }


    // login-merchant unit testing scripts

    @Test
    void loginMerchant_Success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("merchant@example.com");
        request.setPassword("Password@123");

        MerchantResponse merchantResponse = new MerchantResponse();
        merchantResponse.setEmail("merchant@example.com");
        merchantResponse.setBusinessName("Test Business");
        merchantResponse.setOwnerName("John Doe");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken("mock-jwt-token");
        loginResponse.setRefreshToken("mock-refresh-token");
        loginResponse.setTokenType("Bearer");
        loginResponse.setExpiresIn(3600L);

        when(merchantService.loginMerchant(any(LoginRequest.class))).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));

        // Verify service was called
        verify(merchantService, times(1)).loginMerchant(any(LoginRequest.class));
    }

    @Test
    void loginMerchant_WithEmptyEmail() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("Password@123");

        // Act & Assert
        mockMvc.perform(post("/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).loginMerchant(any(LoginRequest.class));
    }

    @Test
    void loginMerchant_WithEmptyPassword() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("merchant@example.com");
        request.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).loginMerchant(any(LoginRequest.class));
    }

    @Test
    void loginMerchant_WithNullFields() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail(null);
        request.setPassword(null);

        // Act & Assert
        mockMvc.perform(post("/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).loginMerchant(any(LoginRequest.class));
    }


    // get-merchant unit testing scripts

    @Test
    void getMerchant_Success() throws Exception {
        // Arrange
        Long merchantId = 1L;
        MerchantResponse merchantResponse = new MerchantResponse();
        merchantResponse.setId(merchantId);
        merchantResponse.setEmail("merchant@example.com");
        merchantResponse.setBusinessName("Test Business");
        merchantResponse.setOwnerName("John Doe");

        when(merchantService.getMerchantById(merchantId)).thenReturn(merchantResponse);

        // Act & Assert
        mockMvc.perform(get("/merchants/{merchantId}", merchantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(merchantId))
                .andExpect(jsonPath("$.data.email").value("merchant@example.com"));

        // Verify service was called
        verify(merchantService, times(1)).getMerchantById(merchantId);
    }

    @Test
    void getMerchant_WithDifferentId() throws Exception {
        // Arrange
        Long merchantId = 5L;
        MerchantResponse merchantResponse = new MerchantResponse();
        merchantResponse.setId(merchantId);
        merchantResponse.setEmail("another@example.com");
        merchantResponse.setBusinessName("Another Business");

        when(merchantService.getMerchantById(merchantId)).thenReturn(merchantResponse);

        // Act & Assert
        mockMvc.perform(get("/merchants/{merchantId}", merchantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(merchantId));

        // Verify service was called
        verify(merchantService, times(1)).getMerchantById(merchantId);
    }


    // update-merchant unit testing scripts

    @Test
    void updateMerchant_Success() throws Exception {
        // Arrange
        Long merchantId = 1L;
        UpdateMerchantRequest request = new UpdateMerchantRequest();
        request.setBusinessName("Updated Business");
        request.setOwnerName("Jane Doe");
        request.setPhoneNumber("+94771234567");

        MerchantResponse merchantResponse = new MerchantResponse();
        merchantResponse.setId(merchantId);
        merchantResponse.setEmail("merchant@example.com");
        merchantResponse.setBusinessName("Updated Business");
        merchantResponse.setOwnerName("Jane Doe");

        when(merchantService.updateMerchant(any(Long.class), any(UpdateMerchantRequest.class))).thenReturn(merchantResponse);

        // Act & Assert
        mockMvc.perform(put("/merchants/{merchantId}", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Merchant updated successfully"))
                .andExpect(jsonPath("$.data.businessName").value("Updated Business"));

        // Verify service was called
        verify(merchantService, times(1)).updateMerchant(merchantId, request);
    }

    @Test
    void updateMerchant_WithInvalidEmail() throws Exception {
        // Arrange
        Long merchantId = 1L;
        UpdateMerchantRequest request = new UpdateMerchantRequest();
        request.setEmail("invalid-email");
        request.setBusinessName("Updated Business");

        // Act & Assert
        mockMvc.perform(put("/merchants/{merchantId}", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).updateMerchant(any(Long.class), any(UpdateMerchantRequest.class));
    }

    @Test
    void updateMerchant_WithEmptyFields() throws Exception {
        // Arrange
        Long merchantId = 1L;
        UpdateMerchantRequest request = new UpdateMerchantRequest();
        // All fields are optional, so empty is valid

        MerchantResponse merchantResponse = new MerchantResponse();
        merchantResponse.setId(merchantId);
        merchantResponse.setEmail("merchant@example.com");

        when(merchantService.updateMerchant(any(Long.class), any(UpdateMerchantRequest.class))).thenReturn(merchantResponse);

        // Act & Assert
        mockMvc.perform(put("/merchants/{merchantId}", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify service was called
        verify(merchantService, times(1)).updateMerchant(merchantId, request);
    }


    // verify-token unit testing scripts

    @Test
    void verifyToken_Success() throws Exception {
        // Arrange
        VerifyTokenRequest request = new VerifyTokenRequest();
        request.setToken("valid-jwt-token");

        TokenVerifyResponse tokenResponse = new TokenVerifyResponse();
        tokenResponse.setValid(true);
        tokenResponse.setEmail("merchant@example.com");
        tokenResponse.setUserId(1L);
        tokenResponse.setUserType("MERCHANT");

        when(merchantService.verifyToken(anyString())).thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/merchants/verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token verified"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.email").value("merchant@example.com"));

        // Verify service was called
        verify(merchantService, times(1)).verifyToken("valid-jwt-token");
    }

    @Test
    void verifyToken_WithEmptyToken() throws Exception {
        // Arrange
        VerifyTokenRequest request = new VerifyTokenRequest();
        request.setToken("");

        // Act & Assert
        mockMvc.perform(post("/merchants/verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).verifyToken(anyString());
    }

    @Test
    void verifyToken_WithNullToken() throws Exception {
        // Arrange
        VerifyTokenRequest request = new VerifyTokenRequest();
        request.setToken(null);

        // Act & Assert
        mockMvc.perform(post("/merchants/verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).verifyToken(anyString());
    }


    // refresh-token unit testing scripts

    @Test
    void refreshToken_Success() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        RefreshTokenResponse refreshResponse = new RefreshTokenResponse();
        refreshResponse.setToken("new-jwt-token");
        refreshResponse.setRefreshToken("new-refresh-token");
        refreshResponse.setTokenType("Bearer");
        refreshResponse.setExpiresIn(3600L);

        when(merchantService.refreshToken(anyString())).thenReturn(refreshResponse);

        // Act & Assert
        mockMvc.perform(post("/merchants/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));

        // Verify service was called
        verify(merchantService, times(1)).refreshToken("valid-refresh-token");
    }

    @Test
    void refreshToken_WithEmptyToken() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("");

        // Act & Assert
        mockMvc.perform(post("/merchants/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).refreshToken(anyString());
    }

    @Test
    void refreshToken_WithNullToken() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(null);

        // Act & Assert
        mockMvc.perform(post("/merchants/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(merchantService, never()).refreshToken(anyString());
    }
}
