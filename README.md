# GenePay Payment Service

A comprehensive Spring Boot microservice that handles payment processing, user authentication, merchant management, and biometric-based transactions for the GenePay platform.

## 📋 Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Technologies](#technologies)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [Environment Variables](#environment-variables)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [Email Templates](#email-templates)

## 🎯 Overview

GenePay Payment Service is a backend microservice that provides:
- **User Management**: Registration, authentication, and email verification
- **Merchant Management**: Merchant registration, authentication, and profile management
- **Payment Processing**: Biometric-verified payment transactions
- **Card Management**: Link and manage payment cards for users and merchants
- **Platform Administration**: Dashboard metrics, user/merchant management, and transaction monitoring
- **Blockchain Integration**: Audit trail for transactions via blockchain relay
- **Email Notifications**: Verification codes and welcome emails via SMTP

## 📁 Project Structure

```
genepay-payment-service/
├── src/
│   ├── main/
│   │   ├── java/com/genepay/genepaypaymentservice/
│   │   │   ├── config/              # Configuration classes (Security, CORS, etc.)
│   │   │   ├── controller/          # REST API controllers
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── CardController.java
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── MerchantController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   ├── PlatformController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── exception/           # Custom exceptions and handlers
│   │   │   ├── model/               # JPA entities
│   │   │   ├── repository/          # Spring Data JPA repositories
│   │   │   ├── service/             # Business logic services
│   │   │   ├── util/                # Utility classes
│   │   │   └── GenepayPaymentServiceApplication.java
│   │   └── resources/
│   │       ├── db/migration/        # Flyway database migrations
│   │       ├── templates/email/     # Thymeleaf email templates
│   │       └── application.yaml     # Application configuration
│   └── test/                        # Unit and integration tests
├── docker-compose.yml               # Docker Compose for PostgreSQL
├── pom.xml                          # Maven dependencies
└── README.md
```

## 🛠 Technologies

- **Java 21**
- **Spring Boot 3.5.7**
  - Spring Security (JWT authentication)
  - Spring Data JPA
  - Spring Web
  - Spring WebFlux (WebClient for HTTP calls)
  - Spring Mail
  - Spring Actuator
- **PostgreSQL 15** (Database)
- **Flyway** (Database migrations)
- **JWT (JJWT)** (Token-based authentication)
- **Lombok** (Reduce boilerplate code)
- **ModelMapper** (Object mapping)
- **Springdoc OpenAPI** (Swagger UI)
- **Thymeleaf** (Email templates)
- **Docker** (Containerization)
- **Maven** (Build tool)

## ✅ Prerequisites

- Java 21 or higher
- Maven 3.8+
- Docker & Docker Compose (for PostgreSQL)
- PostgreSQL 15+ (if not using Docker)
- SMTP server credentials (for email features)

## 🚀 Setup & Installation

### 1. Clone the Repository

```bash
cd modules/genepay-payment-service
```

### 2. Create Environment File

Create a `.env` file in the project root or set environment variables directly:

```bash
# Server Configuration
SERVER_PORT=8080

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=genepay_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production-minimum-256-bits
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Email Configuration (Gmail example)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
EMAIL_FROM=noreply@genepay.com
EMAIL_FROM_NAME=GenePay

# Biometric Service
BIOMETRIC_SERVICE_URL=http://localhost:8001
BIOMETRIC_SERVICE_TIMEOUT=30000

# Banking Service
BANKING_SERVICE_URL=http://localhost:5000
BANKING_SERVICE_TIMEOUT=10000
BIOPAY_PLATFORM_TOKEN=your-platform-token-here

# Blockchain Configuration
BLOCKCHAIN_ENABLED=true
BLOCKCHAIN_RELAY_URL=http://localhost:3001
BLOCKCHAIN_RELAY_TIMEOUT=30000

# Security
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
MAX_LOGIN_ATTEMPTS=5
LOCKOUT_DURATION=900000
```

### 3. Install Dependencies

```bash
mvn clean install
```

## 🗄 Database Setup

### Using Docker Compose (Recommended)

```bash
# Start PostgreSQL container
docker-compose up -d

# Verify container is running
docker ps

# View logs
docker logs genepay-payment-postgres
```

The Docker Compose setup will:
- Create a PostgreSQL 15 database on port `5432`
- Database name: `genepay_db`
- Username: `postgres`
- Password: `postgres`
- Include health checks
- Persist data in a Docker volume

### Using Local PostgreSQL

If you prefer to use a local PostgreSQL installation:

```bash
# Create database
createdb genepay_db

# Or using psql
psql -U postgres
CREATE DATABASE genepay_db;
```

Flyway will automatically run migrations on application startup.

## ▶️ Running the Application

### Using Maven

```bash
# Run the application
mvn spring-boot:run

# Or build and run the JAR
mvn clean package
java -jar target/genepay-payment-service-0.0.1-SNAPSHOT.jar
```

### Using Docker (PostgreSQL only)

```bash
# Start PostgreSQL
docker-compose up -d

# Run the Spring Boot app locally
mvn spring-boot:run
```

The application will start on `http://localhost:8080` (or the port specified in your environment variables).

## 📚 API Documentation

### Accessing Swagger UI

Once the application is running, you can access the interactive API documentation at:

**Swagger UI:** [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)

**OpenAPI JSON:** [http://localhost:8080/api/v1/api-docs](http://localhost:8080/api/v1/api-docs)

### Features of Swagger UI

- **Interactive API Testing**: Test all endpoints directly from the browser
- **Request/Response Examples**: See sample payloads for each endpoint
- **Authentication**: Add JWT Bearer tokens for protected endpoints
- **Schemas**: View all DTOs and models
- **Organized by Controllers**: Endpoints grouped by functionality

### Using Authentication in Swagger

1. **Login** via `/api/v1/users/login` or `/api/v1/merchants/login` or `/api/v1/admin/login`
2. Copy the JWT token from the response
3. Click the **"Authorize"** button in Swagger UI (top right)
4. Enter: `Bearer <your-jwt-token>`
5. Click **"Authorize"**
6. All subsequent requests will include the token

## 🔌 API Endpoints

Base URL: `http://localhost:8080/api/v1`

### Health Check

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/health` | Service health status | No |

### User Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/users/send-verification-code` | Send email verification code | No |
| POST | `/users/register` | Register a new user | No |
| POST | `/users/login` | User login | No |
| POST | `/users/verify-email` | Verify email with code | No |

### Merchant Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/merchants/send-verification-code` | Send merchant verification code | No |
| POST | `/merchants/verify-email` | Verify merchant email | No |
| POST | `/merchants/register` | Register a new merchant | No |
| POST | `/merchants/login` | Merchant login | No |
| GET | `/merchants/{merchantId}` | Get merchant details | Yes |
| PUT | `/merchants/{merchantId}` | Update merchant profile | Yes |
| POST | `/merchants/verify-token` | Verify JWT token validity | Yes |
| POST | `/merchants/refresh-token` | Refresh JWT token | Yes |

### Payment Processing

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/payments/initiate` | Initiate a payment transaction | Yes |
| POST | `/payments/verify` | Verify payment with biometrics | Yes |
| POST | `/payments/{transactionId}/refund` | Refund a transaction | Yes |
| GET | `/payments/{transactionId}` | Get transaction details | Yes |
| GET | `/payments/user/{userId}` | Get user's transaction history | Yes |
| GET | `/payments/user/{userId}/total-spends` | Get user's total spending | Yes |
| GET | `/payments/merchant/{merchantId}` | Get merchant's transactions | Yes |
| POST | `/payments/identify-user` | Identify user via biometric | Yes |
| GET | `/payments/blockchain/health` | Check blockchain relay health | Yes |
| GET | `/payments/blockchain/stats` | Get blockchain statistics | Yes |

### Card Management

#### User Cards

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/cards/user/{userId}` | Link a card to user | Yes |
| GET | `/cards/user/{userId}` | Get all user cards | Yes |
| GET | `/cards/user/{userId}/default` | Get user's default card | Yes |
| PUT | `/cards/user/{userId}/{cardId}/set-default` | Set default card | Yes |
| DELETE | `/cards/user/{userId}/{cardId}` | Remove a card | Yes |
| PUT | `/cards/user/{userId}/{cardId}/nickname` | Update card nickname | Yes |

#### Merchant Cards

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/cards/merchant/{merchantId}` | Link a card to merchant | Yes |
| GET | `/cards/merchant/{merchantId}` | Get all merchant cards | Yes |
| GET | `/cards/merchant/{merchantId}/default` | Get merchant's default card | Yes |
| PUT | `/cards/merchant/{merchantId}/{cardId}/set-default` | Set default card | Yes |
| DELETE | `/cards/merchant/{merchantId}/{cardId}` | Remove a card | Yes |

### Platform & Analytics

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/platform/balance` | Get platform balance | Yes (Admin) |
| GET | `/platform/fees/summary` | Get fee summary | Yes (Admin) |
| GET | `/platform/transactions/statistics` | Get transaction statistics | Yes (Admin) |

### Admin Operations

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/admin/login` | Admin login | No |
| GET | `/admin/{adminId}` | Get admin profile | Yes (Admin) |
| GET | `/admin/dashboard` | Get dashboard metrics | Yes (Admin) |
| GET | `/admin/users` | List all users | Yes (Admin) |
| GET | `/admin/merchants` | List all merchants | Yes (Admin) |
| GET | `/admin/transactions` | List all transactions | Yes (Admin) |

## 🔐 Security

### Authentication

The service uses JWT (JSON Web Tokens) for authentication:

1. **Login** via appropriate endpoint (`/users/login`, `/merchants/login`, `/admin/login`)
2. Receive a JWT token in the response
3. Include the token in subsequent requests: `Authorization: Bearer <token>`

### Token Expiration

- **Access Token**: 24 hours (configurable via `JWT_EXPIRATION`)
- **Refresh Token**: 7 days (configurable via `JWT_REFRESH_EXPIRATION`)

### Password Security

- Passwords are hashed using BCrypt
- Minimum password requirements should be enforced at the application level

### CORS Configuration

Configure `ALLOWED_ORIGINS` environment variable to specify which domains can access the API.

### Rate Limiting

- Maximum login attempts: 5 (configurable via `MAX_LOGIN_ATTEMPTS`)
- Lockout duration: 15 minutes (configurable via `LOCKOUT_DURATION`)

## 📧 Email Templates

The service includes Thymeleaf templates for email notifications:

- **Verification Code Email**: `src/main/resources/templates/email/verification-code.html`
- **Welcome Email**: `src/main/resources/templates/email/welcome.html`

### Gmail SMTP Setup

1. Enable 2-Factor Authentication on your Gmail account
2. Generate an App Password: [Google Account Settings](https://myaccount.google.com/apppasswords)
3. Use the App Password in the `MAIL_PASSWORD` environment variable

## 🔍 Monitoring & Health

### Actuator Endpoints

The service includes Spring Boot Actuator for monitoring:

- **Health**: `http://localhost:8080/api/v1/actuator/health`
- **Info**: `http://localhost:8080/api/v1/actuator/info`
- **Metrics**: `http://localhost:8080/api/v1/actuator/metrics`

### Custom Health Check

- **Service Health**: `http://localhost:8080/api/v1/health`

## 🐳 Docker Support

### PostgreSQL Docker Container

```bash
# Start
docker-compose up -d

# Stop
docker-compose down

# Stop and remove volumes
docker-compose down -v

# View logs
docker logs -f genepay-payment-postgres

# Access PostgreSQL shell
docker exec -it genepay-payment-postgres psql -U postgres -d genepay_db
```

## 🛠 Development

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`:

- **Naming Convention**: `V{version}__{description}.sql`
- **Example**: `V1__Initial_Schema.sql`

To create a new migration:

```bash
# Create a new file following the naming convention
touch src/main/resources/db/migration/V2__Add_User_Preferences.sql
```

Flyway will automatically detect and apply new migrations on startup.

### Building for Production

```bash
# Build JAR file
mvn clean package -DskipTests

# Run the JAR
java -jar target/genepay-payment-service-0.0.1-SNAPSHOT.jar
```

## 🔧 Troubleshooting

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check database logs
docker logs genepay-payment-postgres

# Verify database exists
docker exec -it genepay-payment-postgres psql -U postgres -c "\l"
```

### JWT Issues

- Ensure `JWT_SECRET` is at least 256 bits (32+ characters)
- Verify token expiration settings
- Check system time synchronization

### Email Issues

- Verify SMTP credentials
- Check firewall/network settings
- Enable "Less secure app access" or use App Passwords for Gmail

### Port Already in Use

```bash
# Change port in .env or application.yaml
SERVER_PORT=8081

# Or kill process using the port
lsof -ti:8080 | xargs kill -9
```

## 📝 License

[Add your license information here]

## 👥 Contributors

[Add contributor information here]

## 📞 Support

For issues and questions:
- Email: support@genepay.com
- GitHub Issues: [Create an issue](https://github.com/your-repo/issues)

---

**Last Updated**: February 26, 2026  
**Version**: 0.0.1-SNAPSHOT
