# Data Transfer Object (DTO) Layer

This folder contains DTO classes used for data transfer between layers and API requests/responses.

## Responsibilities

- Transfer data between different layers
- Define API request/response structures
- Encapsulate data for external communication
- Hide internal entity structure from clients

## Structure

- `PaymentRequest.java` - Payment creation request DTO
- `PaymentResponse.java` - Payment response DTO
- `TransactionDTO.java` - Transaction data transfer object
- `ErrorResponse.java` - Error response DTO

## Naming Convention

All DTOs should follow the naming pattern: `*DTO.java`, `*Request.java`, `*Response.java`

Example:
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String userId;
    private BigDecimal amount;
    private String description;
}
```
