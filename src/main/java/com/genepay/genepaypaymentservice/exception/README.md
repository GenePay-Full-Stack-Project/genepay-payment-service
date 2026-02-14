# Exception Handling Layer

This folder contains custom exception classes.

## Responsibilities

- Define custom exception types
- Provide meaningful error messages
- Handle exception globally
- Map exceptions to appropriate HTTP status codes

## Structure

- `PaymentException.java` - Base payment exception
- `InsufficientFundsException.java` - Thrown when payment fails due to insufficient funds
- `PaymentNotFoundException.java` - Thrown when payment record not found
- `InvalidPaymentException.java` - Thrown when payment data is invalid

## Naming Convention

All exceptions should follow the naming pattern: `*Exception.java`

Example:
```java
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
```
