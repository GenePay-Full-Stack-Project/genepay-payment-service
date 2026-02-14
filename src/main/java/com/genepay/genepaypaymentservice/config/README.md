# Configuration Layer

This folder contains Spring configuration classes.

## Responsibilities

- Define Spring beans
- Configure application properties
- Setup database connections
- Configure security, logging, and other cross-cutting concerns
- Setup external service integrations

## Structure

- `DatabaseConfig.java` - Database configuration
- `SecurityConfig.java` - Security and authentication configuration
- `CorsConfig.java` - CORS configuration
- `JacksonConfig.java` - JSON serialization configuration

## Naming Convention

All configuration classes should follow the naming pattern: `*Config.java`

Example:
```java
@Configuration
public class DatabaseConfig {
    @Bean
    public DataSource dataSource() {
        // database configuration
        return null;
    }
}
```
