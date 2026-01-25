package com.genepay.genepaypaymentservice.exception;

public class BiometricVerificationException extends RuntimeException {
    public BiometricVerificationException(String message) {
        super(message);
    }
    
    public BiometricVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
