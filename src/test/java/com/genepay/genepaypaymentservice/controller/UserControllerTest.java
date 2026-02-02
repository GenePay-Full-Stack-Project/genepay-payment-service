package com.genepay.genepaypaymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genepay.genepaypaymentservice.dto.SendVerificationCodeRequest;
import com.genepay.genepaypaymentservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void sendVerificationCode_Success() throws Exception {
        // Arrange
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("test@example.com");

        // Act & Assert
        mockMvc.perform(post("/users/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification code sent successfully"));

        // Verify service was called
        verify(userService, times(1)).sendVerificationCode("test@example.com");
    }

    @Test
    void sendVerificationCode_WithInvalidEmail() throws Exception {
        // Arrange
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/users/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).sendVerificationCode(anyString());
    }

    @Test
    void sendVerificationCode_WithEmptyEmail() throws Exception {
        // Arrange
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("");

        // Act & Assert
        mockMvc.perform(post("/users/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).sendVerificationCode(anyString());
    }

    @Test
    void sendVerificationCode_WithNullEmail() throws Exception {
        // Arrange
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/users/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).sendVerificationCode(anyString());
    }
}
