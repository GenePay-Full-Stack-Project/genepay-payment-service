package com.genepay.genepaypaymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genepay.genepaypaymentservice.dto.SendVerificationCodeRequest;
import com.genepay.genepaypaymentservice.dto.UserRegistrationRequest;
import com.genepay.genepaypaymentservice.dto.UserResponse;
import com.genepay.genepaypaymentservice.dto.VerifyEmailRequest;
import com.genepay.genepaypaymentservice.service.UserService;
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
 
    
    
   
    // login-user

    // send-verification-code unit testing scripts 

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


    // verify-email unit testing scripts 

    @Test
    void verifyEmail_Success() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setVerificationCode("123456");

        // Act & Assert
        mockMvc.perform(post("/users/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        // Verify service was called
        verify(userService, times(1)).verifyEmail(any(VerifyEmailRequest.class));
    }

    @Test
    void verifyEmail_WithInvalidEmail() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("invalid-email");
        request.setVerificationCode("123456");

        // Act & Assert
        mockMvc.perform(post("/users/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).verifyEmail(any(VerifyEmailRequest.class));
    }

    @Test
    void verifyEmail_WithEmptyEmail() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("");
        request.setVerificationCode("123456");

        // Act & Assert
        mockMvc.perform(post("/users/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).verifyEmail(any(VerifyEmailRequest.class));
    }

    @Test
    void verifyEmail_WithEmptyVerificationCode() throws Exception {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setVerificationCode("");

        // Act & Assert
        mockMvc.perform(post("/users/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).verifyEmail(any(VerifyEmailRequest.class));
    }


     // register-user unit testing scripts 

    @Test
    void registerUser_Success() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setFullName("John Doe");
        request.setPassword("Password123!");
        request.setNicNumber("123456789V");
        request.setPhoneNumber("+94771234567");

        UserResponse userResponse = new UserResponse();
        userResponse.setEmail("test@example.com");
        userResponse.setFullName("John Doe");
        userResponse.setNicNumber("123456789V");

        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        // Verify service was called
        verify(userService, times(1)).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUser_WithInvalidEmail() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("invalid-email");
        request.setFullName("John Doe");
        request.setPassword("Password123!");
        request.setNicNumber("123456789V");
        request.setPhoneNumber("+94771234567");

        // Act & Assert
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUser_WithEmptyFields() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("");
        request.setFullName("");
        request.setPassword("");
        request.setNicNumber("");
        request.setPhoneNumber("");

        // Act & Assert
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUser_WithNullEmail() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail(null);
        request.setFullName("John Doe");
        request.setPassword("Password123!");
        request.setNicNumber("123456789V");
        request.setPhoneNumber("+94771234567");

        // Act & Assert
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was not called
        verify(userService, never()).registerUser(any(UserRegistrationRequest.class));
    }
}
