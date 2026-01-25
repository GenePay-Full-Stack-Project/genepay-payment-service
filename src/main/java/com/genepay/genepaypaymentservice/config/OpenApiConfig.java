package com.biopay.paymentservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.name:payment-service}")
    private String appName;

    @Value("${app.version:0.0.1}")
    private String appVersion;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("BioPay Payment Service API")
                        .version(appVersion)
                        .description("Biometric-based payment system - Payment Service microservice for handling user registration, merchant onboarding, and payment processing with custom Banking System integration. All payments are automatically split: 97% to merchant, 3% to platform.")
                        .contact(new Contact()
                                .name("BioPay Development Team")
                                .email("dev@biopay.com")
                                .url("https://github.com/Uni-DevNet/bio_pay_system"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://biopay.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.facewallet-payment.corszero.com" + contextPath)
                                .description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from login endpoint")));
    }
}
