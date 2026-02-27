package com.genepay.genepaypaymentservice.service;

import com.genepay.genepaypaymentservice.dto.GoogleUserInfo;
import com.genepay.genepaypaymentservice.exception.BadRequestException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Verify Google ID token and extract user information
     * @param idToken Google ID token from client
     * @return GoogleUserInfo containing user details
     * @throws BadRequestException if token is invalid
     */
    public GoogleUserInfo verifyGoogleToken(String idToken) {
        try {
            log.info("Verifying Google ID token");
            
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            
            if (googleIdToken == null) {
                log.error("Invalid Google ID token");
                throw new BadRequestException("Invalid Google ID token");
            }

            Payload payload = googleIdToken.getPayload();
            
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            log.info("Google token verified successfully for user: {}", email);

            return GoogleUserInfo.builder()
                    .googleId(googleId)
                    .email(email)
                    .name(name)
                    .picture(pictureUrl)
                    .emailVerified(emailVerified)
                    .build();

        } catch (Exception e) {
            log.error("Failed to verify Google token", e);
            throw new BadRequestException("Failed to verify Google token: " + e.getMessage());
        }
    }
}
