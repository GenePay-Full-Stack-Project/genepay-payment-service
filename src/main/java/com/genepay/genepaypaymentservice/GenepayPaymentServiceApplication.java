package com.genepay.genepaypaymentservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GenepayPaymentServiceApplication {

    public static void main(String[] args) {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        } catch (Exception e) {
            System.out.println("No .env file found, using system environment variables");
        }
        SpringApplication.run(GenepayPaymentServiceApplication.class, args);
    }
}


