package com.genepay.genepaypaymentservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor

public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

        @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.email.support-email}")
    private String supportEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String verificationCode) {
        try {
            log.info("Preparing to send verification email to: {}", toEmail);

            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("year", LocalDateTime.now().getYear());
            context.setVariable("expiryTime", "24 hours");

            String htmlContent = templateEngine.process("email/verification-code", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your BioPay Account - Verification Code");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Verification email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            log.info("Preparing to send welcome email to: {}", toEmail);

            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("year", LocalDateTime.now().getYear());

            String htmlContent = templateEngine.process("email/welcome", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to BioPay - Your Account is Verified!");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome email failures
        }
    }
}
