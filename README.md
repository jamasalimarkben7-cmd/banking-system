# Northstar Banking Backend

A comprehensive, enterprise-grade banking management system built on Spring Boot 3.5, Java 21, Spring Data JPA, and Thymeleaf. Designed for academic demonstration of modern full-stack architecture, secure authentication, and transactional integrity.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Features](#features)
- [Technologies](#technologies)
- [Architecture](#architecture)
- [Project Directory Structure](#project-directory-structure)
- [Database Design](#database-design)
- [Setup Instructions](#setup-instructions)
- [Environment Variables](#environment-variables)
- [Running the Application](#running-the-application)
- [Authentication Flow](#authentication-flow)
- [Security Notes](#security-notes)
- [API Documentation](#api-documentation)
- [Automated Tests](#automated-tests)
- [Banking Transaction Flows](#banking-transaction-flows)

---

## Project Overview

The Northstar Banking Backend is a monolithic server-rendered banking application that provides core banking operations including user authentication, account management, deposits, withdrawals, transfers between accounts, and transaction history. It demonstrates Spring Security-based authentication with role-based access control, pessimistic locking for concurrent transaction safety, and ACID-compliant transactional integrity.

### Key Characteristics

- **Server-rendered** using Thymeleaf templates
- **Database-agnostic** — supports H2 (development) and PostgreSQL (production)
- **Transaction-safe** — uses pessimistic write locking and `@Transactional` propagation
- **Secure** — BCrypt password hashing, Spring Security form authentication, authorization checks on all account operations

---

## Features

| Feature | Description |
|---------|-------------|
| **User Authentication** | Secure login with BCrypt-hashed passwords, session-based authentication via Spring Security |
| **Account Management** | View all accounts linked to the authenticated user |
| **Deposits** | Credit funds to a user-owned account with transaction recording |
| **Withdrawals** | Debit funds with insufficient balance protection |
| **Transfers** | Atomically move funds between accounts with rollback on failure |
| **Transaction History** | View recent transactions across user's accounts |
| **Role-Based Access** | CLIENT and ADMIN roles with ownership-based authorization |
| **Transaction Rollback** | Automatic rollback on any failure during multi-step operations |

---

## Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Spring Boot** | 3.5.0 | Application framework |
| **Java** | 21 (LTS) | Language / Runtime |
| **Spring Data JPA** | 6.x | ORM / Data access |
| **Spring Security** | 6.x | Authentication & Authorization |
| **Thymeleaf** | 3.x | Server-side templating |
| **PostgreSQL** | 16+ | Production database |
| **H2 Database** | 2.x | In-memory development database |
| **HikariCP** | 5.x | Connection pooling |
| **JUnit 5** | 5.10.x | Unit testing |
| **Mockito** | 5.x | Mocking framework |
| **BCrypt** | — | Password hashing |
| **Maven** | 3.9+ | Build tool |

---

## Architecture

The application follows a **layered architecture** with clear separation of concerns:

```
┌──────────────────────────────────────────────────────────┐
│                    Controller Layer                       │
│  (DashboardController, AuthController)                    │
│  Handles HTTP requests, form validation, view resolution  │
├──────────────────────────────────────────────────────────┤
│                    Service Layer                          │
│  (BankingService)                                        │
│  Business logic, transactional boundaries, security checks │
├──────────────────────────────────────────────────────────┤
│                    Repository Layer                       │
│  (UserRepository, AccountRepository, TransactionRepository)│
│  Data access, JPA queries, pessimistic locking            │
├──────────────────────────────────────────────────────────┤
│                    Model / Entity Layer                   │
│  (User, Account, Transaction, Role, AccountType, etc.)    │
│  JPA entity mappings, domain objects                      │
├──────────────────────────────────────────────────────────┤
│                    Configuration Layer                    │
│  (SecurityConfig, BankingSystemApplication)               │
│  Security rules, application startup, beans               │
└──────────────────────────────────────────────────────────┘
```

---

## Project Directory Structure

```
banking-system/
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/bankingsystem/
│   │   │   ├── BankingSystemApplication.java       # Main entry point, admin user creation
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java             # Spring Security config, BCrypt bean
│   │   │   ├── exception/
│   │   │   │   └── InsufficientBalanceException.java  # Custom exception for insufficient funds
│   │   │   ├── model/
│   │   │   │   ├── User.java                       # User entity (maps to "customers")
│   │   │   │   ├── Account.java                    # Account entity
│   │   │   │   ├── Transaction.java                # Transaction entity
│   │   │   │   ├── Role.java                       # Role enum (CLIENT, ADMIN)
│   │   │   │   ├── AccountType.java                # AccountType enum (CHECKING, SAVINGS)
│   │   │   │   └── TransactionType.java            # TransactionType enum (DEPOSIT, WITHDRAWAL, TRANSFER)
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java             # User data access
│   │   │   │   ├── AccountRepository.java          # Account data access + pessimistic locking
│   │   │   │   └── TransactionRepository.java      # Transaction data access
│   │   │   ├── service/
│   │   │   │   └── BankingService.java             # Core banking logic, transactions, auth checks
│   │   │   └── web/
│   │   │       ├── BankingController.java          # REST API endpoints
│   │   │       ├── TransactionForm.java            # Form backing object
│   │   │       ├── GlobalExceptionHandler.java     # Global exception handling
│   │   │       └── AuthController.java             # Login page controller
│   │   └── resources/
│   │       ├── application.properties              # App configuration
│   │       ├── schema.sql                          # Database schema
│   │       ├── data.sql                            # Seed data
│   │       └── templates/                          # Thymeleaf templates
│   │           ├── fragments/
│   │           │   └── layout.html
│   │           ├── login.html
│   │           ├── dashboard.html
│   │           ├── accounts.html
│   │           ├── transaction-form.html
│   │           └── error.html
│   └── test/
│       └── java/com/bankingsystem/
│           ├── UserRegistrationAuthTest.java
│           ├── DepositWithdrawTest.java
│           ├── TransferTest.java
│           └── AuthorizationTest.java
├── target/                              # Compiled output
├── pom.xml                              # Maven build configuration
├── README.md                            # This file
└── API-DOCUMENTATION.md                 # API reference documentation
```

---

## Database Design

### Entity Relationship Diagram

```mermaid
erDiagram
    USER {
        BIGINT id PK "Auto-generated identity"
        VARCHAR(100) username "Unique, not null"
        VARCHAR(255) password "BCrypt hashed, not null"
        VARCHAR(20) role "CLIENT or ADMIN, default CLIENT"
    }

    ACCOUNT {
        BIGINT id PK "Auto-generated identity"
        VARCHAR(24) account_number "Unique, not null"
        NUMERIC(19,2) balance "Default 0.00"
        VARCHAR(20) account_type "CHECKING or SAVINGS, not null"
        BIGINT user_id FK "References USER(id), not null"
    }

    TRANSACTION {
        BIGINT id PK "Auto-generated identity"
        VARCHAR(20) type "DEPOSIT, WITHDRAWAL, or TRANSFER, not null"
        NUMERIC(19,2) amount "Not null"
        VARCHAR(24) source_account_number "Nullable"
        VARCHAR(24) destination_account_number "Nullable"
        TIMESTAMP created_at "Auto-set on persist, not null"
        BIGINT account_id FK "References ACCOUNT(id), not null"
    }

    USER ||--o{ ACCOUNT : "owns"
    ACCOUNT ||--o{ TRANSACTION : "has"
```

### Table Definitions (from `schema.sql`)

#### `users` Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, GENERATED BY DEFAULT AS IDENTITY | Primary key |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE | Login username |
| `password` | VARCHAR(255) | NOT NULL | BCrypt-hashed password |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'CLIENT' | Account role (CLIENT/ADMIN) |

#### `accounts` Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, GENERATED BY DEFAULT AS IDENTITY | Primary key |
| `account_number` | VARCHAR(24) | NOT NULL, UNIQUE | Account identifier |
| `balance` | NUMERIC(19,2) | NOT NULL, DEFAULT 0.00 | Current balance |
| `account_type` | VARCHAR(20) | NOT NULL | CHECKING or SAVINGS |
| `user_id` | BIGINT | NOT NULL, FK → users(id) | Owning user |

#### `transactions` Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, GENERATED BY DEFAULT AS IDENTITY | Primary key |
| `type` | VARCHAR(20) | NOT NULL | DEPOSIT, WITHDRAWAL, or TRANSFER |
| `amount` | NUMERIC(19,2) | NOT NULL | Transaction amount |
| `source_account_number` | VARCHAR(24) | Nullable | Source account (transfers) |
| `destination_account_number` | VARCHAR(24) | Nullable | Destination account (transfers) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Timestamp |
| `account_id` | BIGINT | NOT NULL, FK → accounts(id) | Associated account |

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `idx_transactions_account_id` | transactions(account_id) | Fast lookup by account |
| `idx_transactions_created_at` | transactions(created_at DESC) | Fast recent transaction queries |
| `idx_accounts_user_id` | accounts(user_id) | Fast lookup by user |

### Seed Data (`data.sql`)

| User | Password | Accounts |
|------|----------|----------|
| `customer` | `password123` | CHK000001 (Checking, $5,000.00), SAV000001 (Savings, $10,000.00) |
| `admin` | `admin123` | ADM000001 (Checking, $100,000.00) |

---

## Setup Instructions

### Prerequisites

- **Java 21 JDK** (Temurin recommended)
- **Apache Maven 3.9+**
- **PostgreSQL 16+** (for production) or use built-in H2 (development)

### Step 1: Clone and Navigate

```bash
cd banking-system
```

### Step 2: Configure Database

Edit `src/main/resources/application.properties` or set environment variables:

```bash
# For H2 (development, default) — no configuration needed
# Database URL: jdbc:h2:mem:banking_system

# For PostgreSQL (production)
export DB_URL="jdbc:postgresql://localhost:5432/banking_system"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
export DB_DRIVER="org.postgresql.Driver"
```

### Step 3: Run the Application

```bash
# Using Maven wrapper
./mvnw clean spring-boot:run

# Or using system Maven
mvn clean spring-boot:run
```

### Step 4: Access the Application

- **Login page**: http://localhost:8080/login
- **H2 Console** (development): http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:banking_system`
  - Username: `sa`
  - Password: (empty)

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:h2:mem:banking_system;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | Database connection URL |
| `DB_USERNAME` | `sa` | Database username |
| `DB_PASSWORD` | *(empty)* | Database password |
| `DB_DRIVER` | `org.h2.Driver` | JDBC driver class |
| `DB_POOL_MAX_SIZE` | `10` | Maximum connection pool size |
| `DB_POOL_MIN_IDLE` | `2` | Minimum idle connections |
| `JPA_DDL_AUTO` | `update` | JPA schema strategy |

### `.env.example` Template

```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/banking_system
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password
DB_DRIVER=org.postgresql.Driver

# Connection Pool
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2

# JPA
JPA_DDL_AUTO=update
```

---

## Running the Application

### Development (H2 In-Memory)

```bash
./mvnw clean spring-boot:run
```

Access at http://localhost:8080/login

### Production (PostgreSQL)

```bash
export DB_URL="jdbc:postgresql://localhost:5432/banking_system"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
export DB_DRIVER="org.postgresql.Driver"

./mvnw clean spring-boot:run
```

### Running Tests

```bash
./mvnw test
```

### Building the JAR

```bash
./mvnw clean package
java -jar target/banking-system-0.0.1-SNAPSHOT.jar
```

---

## Authentication Flow

### Login Process

1. User navigates to `/login`
2. Submits username and password via Spring Security form login
3. `SecurityConfig` authenticates via `UserDetailsService` which loads user from `UserRepository`
4. BCrypt password comparison validates credentials
5. On success: redirected to `/dashboard`
6. On failure: redirected to `/login?error`

### User Details Service (`SecurityConfig`)

```java
UserDetailsService userDetailsService = username ->
    userRepository.findByUsername(username)
        .map(user -> User.withUsername(user.getUsername())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .build())
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
```

### Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Client | `customer` | `password123` |
| Admin | `admin` | `admin123` |

---

## Security Notes

### Password Security

- All passwords are hashed using **BCrypt** (`BCryptPasswordEncoder` with strength 10)
- The `@Primary` bean overrides Spring Boot's default password encoder
- Passwords are never stored in plaintext

### Authorization

- All endpoints except `/login` and `/css/**` require authentication
- Account operations check **ownership** via `getOwnedAccountForUpdate()`:
  - Fetches account with **pessimistic write lock** (`LockModeType.PESSIMISTIC_WRITE`)
  - Verifies `account.getUser().getUsername()` matches the authenticated user
  - Throws `SecurityException` if user does not own the account
- The `GlobalExceptionHandler` catches `SecurityException` and returns the error view

### Session Management

- Spring Security manages HTTP sessions
- Form-based authentication with CSRF protection enabled by default
- Session fixation protection enabled

### Transaction Safety

- All mutating operations are `@Transactional`
- Pessimistic write locks prevent concurrent balance modifications
- Failed transactions automatically roll back

---

## API Documentation

See [API-DOCUMENTATION.md](API-DOCUMENTATION.md) for the complete REST API reference including all endpoints, request/response formats, and error codes.

---

## Automated Tests

### Test Summary

**41 tests** across 4 test classes, all passing. Tests use **JUnit 5** with **Mockito** for repository mocking.

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `UserRegistrationAuthTest` | 5 | User lookup, registration, duplicate prevention, Spring Security auth |
| `DepositWithdrawTest` | 13 | Deposit/withdraw success, balance updates, validation errors, edge cases |
| `TransferTest` | 12 | Transfer success, rollback scenarios, validation, atomicity |
| `AuthorizationTest` | 11 | Ownership checks, access control, unauthorized operations |

### Running Tests

```bash
./mvnw test
```

### Test Approach

- **Mockito** mocks `AccountRepository`, `TransactionRepository`, and `UserRepository`
- `@InjectMocks` injects mocks into `BankingService`
- `@ExtendWith(MockitoExtension.class)` enables Mockito annotations
- `@BeforeEach` sets up fresh test fixtures for each test
- All tests verify both **state changes** (balance updates) and **interactions** (repository save calls)

---

## Banking Transaction Flows

### Deposit Flow

```
Client → POST /transactions/deposit
  → BankingService.deposit(username, accountNumber, amount)
    → getOwnedAccountForUpdate() [verify ownership + pessimistic lock]
    → validateAmount() [positive, max 2 decimals]
    → account.credit(amount) [update balance]
    → transactionRepository.save(DEPOSIT transaction)
    → COMMIT
  → Redirect to /dashboard?success=deposit
```

### Withdrawal Flow

```
Client → POST /transactions/withdraw
  → BankingService.withdraw(username, accountNumber, amount)
    → getOwnedAccountForUpdate() [verify ownership + pessimistic lock]
    → validateAmount() [positive, max 2 decimals]
    → if balance < amount: throw InsufficientBalanceException → ROLLBACK
    → account.debit(amount) [update balance]
    → transactionRepository.save(WITHDRAWAL transaction)
    → COMMIT
  → Redirect to /dashboard?success=withdraw
```

### Transfer Flow (Atomic)

```
Client → POST /transactions/transfer
  → BankingService.transfer(username, source, destination, amount)
    → if source == destination: throw IllegalArgumentException → ROLLBACK
    → validateAmount() [positive, max 2 decimals]
    → getOwnedAccountForUpdate(username, source) [verify ownership + lock]
    → findByAccountNumberForUpdate(destination) [lock destination]
    → if source.balance < amount: throw InsufficientBalanceException → ROLLBACK
    → source.debit(amount)
    → destination.credit(amount)
    → transactionRepository.save(TRANSFER × 2) [source + destination records]
    → COMMIT (all or nothing)
  → Redirect to /dashboard?success=transfer
```

### Rollback Guarantees

All failure scenarios result in complete transaction rollback:

| Failure Type | Result |
|-------------|--------|
| Insufficient balance | No balances modified, no transactions saved |
| Invalid/destination account | Source unchanged, no transactions saved |
| Same source and destination | Rejected before any processing |
| Invalid amount (zero, negative, too many decimals) | Rejected before any processing |
| Unauthorized access | SecurityException, no changes made |

---

## License

Academic/Educational project.
