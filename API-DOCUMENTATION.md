# Northstar Banking — API Documentation

Comprehensive API reference for the Northstar Banking Backend. This document covers all endpoints, data models, authentication, and error handling.

---

## Table of Contents

- [Base URL](#base-url)
- [Authentication](#authentication)
- [Error Format](#error-format)
- [Endpoints](#endpoints)
  - [Auth](#auth)
  - [Customers (Users)](#customers-users)
  - [Accounts](#accounts)
  - [Deposits](#deposits)
  - [Withdrawals](#withdrawals)
  - [Transfers](#transfers)
  - [Transaction History](#transaction-history)
- [Data Models](#data-models)
- [Status Codes](#status-codes)

---

## Base URL

```
http://localhost:8080
```

## Authentication

The API uses **session-based authentication** via Spring Security. Clients must authenticate before accessing protected resources.

### Login

```http
POST /login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=customer&password=password123
```

On successful login, a session cookie (`JSESSIONID`) is returned. Include this cookie in all subsequent requests.

### Logout

```http
POST /logout HTTP/1.1
Cookie: JSESSIONID=<session-id>
```

### Authorization Header (Bearer Token)

For API-style clients, authentication can also be expressed via Bearer tokens when integrated with a token-producing authentication mechanism:

```http
GET /api/accounts HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json
```

> **Note:** In the default configuration, authentication is handled via Spring Security form login with session cookies. Bearer token support requires integration with a JWT or OAuth2 provider. The API documentation below assumes authenticated sessions.

---

## Error Format

All error responses follow a consistent format:

```json
{
  "error": "Error message describing what went wrong",
  "status": 400
}
```

---

## Endpoints

---

### Auth

#### Login

Authenticate a user and establish a session.

```http
POST /login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=customer&password=password123
```

**Response:** `302 Found` → Redirect to `/dashboard`

| Header | Value |
|--------|-------|
| `Set-Cookie` | `JSESSIONID=<session-id>; Path=/; HttpOnly` |

**Error Responses:**

| Code | Scenario |
|------|----------|
| `401` | Invalid credentials |

---

### Customers (Users)

#### Get Current User Profile

Retrieve information about the authenticated user.

```http
GET /api/users/me HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "username": "customer",
  "role": "CLIENT"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | User ID (auto-generated) |
| `username` | string | Login username |
| `role` | string | Role: `CLIENT` or `ADMIN` |

#### Register New User

Create a new customer account.

```http
POST /api/users HTTP/1.1
Content-Type: application/json

{
  "username": "newuser",
  "password": "securePassword123",
  "role": "CLIENT"
}
```

**Response:** `201 Created`

```json
{
  "id": 5,
  "username": "newuser",
  "role": "CLIENT"
}
```

**Error Responses:**

| Code | Scenario |
|------|----------|
| `400` | Username already exists or invalid input |
| `401` | Not authenticated |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `username` | string | Yes | Unique username (3-100 chars) |
| `password` | string | Yes | Plain text password (hashed with BCrypt) |
| `role` | string | No | Role: `CLIENT` (default) or `ADMIN` |

---

### Accounts

#### List All Accounts for Current User

Retrieve all accounts belonging to the authenticated user.

```http
GET /api/accounts HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json
```

**Response:** `200 OK`

```json
{
  "accounts": [
    {
      "id": 1,
      "accountNumber": "CHK000001",
      "balance": 5000.00,
      "accountType": "CHECKING",
      "user": {
        "id": 1,
        "username": "customer"
      }
    },
    {
      "id": 2,
      "accountNumber": "SAV000001",
      "balance": 10000.00,
      "accountType": "SAVINGS",
      "user": {
        "id": 1,
        "username": "customer"
      }
    }
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | Account ID |
| `accountNumber` | string | Unique account number (24 chars max) |
| `balance` | number | Current balance (19 digits, 2 decimals) |
| `accountType` | string | `CHECKING` or `SAVINGS` |
| `user` | object | Owning user (id, username) |

**Error Responses:**

| Code | Scenario |
|------|----------|
| `401` | Not authenticated |

#### Get Account Details

Retrieve a specific account by account number. Requires account ownership.

```http
GET /api/accounts/CHK000001 HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "accountNumber": "CHK000001",
  "balance": 5000.00,
  "accountType": "CHECKING",
  "user": {
    "id": 1,
    "username": "customer"
  }
}
```

**Error Responses:**

| Code | Scenario |
|------|----------|
| `401` | Not authenticated |
| `403` | User does not own this account |
| `404` | Account not found |

---

### Deposits

#### Deposit Funds

Credit an amount to a user-owned account.

```http
POST /api/deposits HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{
  "accountNumber": "CHK000001",
  "amount": 250.00
}
```

**Response:** `200 OK`

```json
{
  "success": true,
  "message": "Deposit of $250.00 to CHK000001 completed successfully",
  "newBalance": 5250.00
}
```

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accountNumber` | string | Yes | Target account number |
| `amount` | number | Yes | Amount to deposit (must be > 0, max 2 decimals) |

**Error Responses:**

| Code | Scenario |
|------|----------|
| `400` | Invalid amount (zero, negative, too many decimals) |
| `401` | Not authenticated |
| `403` | User does not own this account |
| `404` | Account not found |

---

### Withdrawals

#### Withdraw Funds

Debit an amount from a user-owned account. Protected against overdrafts.

```http
POST /api/withdrawals HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{
  "accountNumber": "CHK000001",
  "amount": 100.00
}
```

**Response:** `200 OK`

```json
{
  "success": true,
  "message": "Withdrawal of $100.00 from CHK000001 completed successfully",
  "newBalance": 4900.00
}
```

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accountNumber` | string | Yes | Source account number |
| `amount` | number | Yes | Amount to withdraw (must be > 0, max 2 decimals) |

**Error Responses:**

| Code | Scenario |
|------|----------|
| `400` | Insufficient balance or invalid amount |
| `401` | Not authenticated |
| `403` | User does not own this account |
| `404` | Account not found |

**Insufficient Balance Error:**

```json
{
  "error": "Insufficient funds",
  "status": 400
}
```

---

### Transfers

#### Transfer Funds Between Accounts

Atomically transfer funds from a source account to a destination account. Both accounts must belong to the authenticated user.

```http
POST /api/transfers HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{
  "accountNumber": "CHK000001",
  "destinationAccountNumber": "SAV000001",
  "amount": 500.00
}
```

**Response:** `200 OK`

```json
{
  "success": true,
  "message": "Transfer of $500.00 from CHK000001 to SAV000001 completed successfully",
  "sourceBalance": 4500.00,
  "destinationBalance": 10500.00
}
```

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accountNumber` | string | Yes | Source account number |
| `destinationAccountNumber` | string | Yes | Destination account number |
| `amount` | number | Yes | Amount to transfer (must be > 0, max 2 decimals) |

**Error Responses:**

| Code | Scenario |
|------|----------|
| `400` | Insufficient balance, same source/dest, invalid amount, or destination not found |
| `401` | Not authenticated |
| `403` | User does not own source account |
| `404` | Account not found |

**Insufficient Balance Error:**

```json
{
  "error": "Insufficient funds",
  "status": 400
}
```

**Rollback Behavior:** If any step of the transfer fails (insufficient funds, destination not found, etc.), the entire transaction is rolled back. Neither account balance is modified and no transaction records are created.

---

### Transaction History

#### List Recent Transactions

Retrieve the last 20 transactions across all accounts owned by the authenticated user.

```http
GET /api/transactions HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json
```

**Response:** `200 OK`

```json
{
  "transactions": [
    {
      "id": 45,
      "type": "TRANSFER",
      "amount": 500.00,
      "sourceAccountNumber": "CHK000001",
      "destinationAccountNumber": "SAV000001",
      "createdAt": "2026-09-23T10:30:00Z",
      "account": {
        "id": 1,
        "accountNumber": "CHK000001"
      }
    },
    {
      "id": 44,
      "type": "DEPOSIT",
      "amount": 250.00,
      "sourceAccountNumber": null,
      "destinationAccountNumber": "CHK000001",
      "createdAt": "2026-09-23T09:15:00Z",
      "account": {
        "id": 1,
        "accountNumber": "CHK000001"
      }
    }
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | Transaction ID |
| `type` | string | `DEPOSIT`, `WITHDRAWAL`, or `TRANSFER` |
| `amount` | number | Transaction amount |
| `sourceAccountNumber` | string/null | Source account (transfers only) |
| `destinationAccountNumber` | string/null | Destination account (transfers/deposits) |
| `createdAt` | string (ISO 8601) | Timestamp of transaction |
| `account` | object | Associated account (id, accountNumber) |

**Error Responses:**

| Code | Scenario |
|------|----------|
| `401` | Not authenticated |

---

## Data Models

### User (Customer)

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE |
| `password` | VARCHAR(255) | NOT NULL (BCrypt) |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'CLIENT' |

### Account

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `account_number` | VARCHAR(24) | NOT NULL, UNIQUE |
| `balance` | NUMERIC(19,2) | NOT NULL, DEFAULT 0.00 |
| `account_type` | VARCHAR(20) | NOT NULL (CHECKING/SAVINGS) |
| `user_id` | BIGINT | NOT NULL, FK → users(id) |

### Transaction

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `type` | VARCHAR(20) | NOT NULL (DEPOSIT/WITHDRAWAL/TRANSFER) |
| `amount` | NUMERIC(19,2) | NOT NULL |
| `source_account_number` | VARCHAR(24) | Nullable |
| `destination_account_number` | VARCHAR(24) | Nullable |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `account_id` | BIGINT | NOT NULL, FK → accounts(id) |

---

## Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| `200` | OK | Successful GET, POST, PUT operations |
| `201` | Created | Resource successfully created |
| `400` | Bad Request | Validation error, insufficient funds, invalid input |
| `401` | Unauthorized | Missing or invalid authentication |
| `403` | Forbidden | User does not own the requested resource |
| `404` | Not Found | Resource does not exist |
| `500` | Internal Server Error | Unexpected server error |

---

## Sample Workflow

### Complete Banking Session

```http
POST /login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=customer&password=password123

→ Cookie: JSESSIONID=abc123
```

```http
GET /api/accounts HTTP/1.1
Cookie: JSESSIONID=abc123

→ 200 OK: Returns checking ($5,000) and savings ($10,000) accounts
```

```http
POST /api/deposits HTTP/1.1
Cookie: JSESSIONID=abc123
Content-Type: application/json

{"accountNumber": "CHK000001", "amount": 250.00}

→ 200 OK: New checking balance = $5,250.00
```

```http
POST /api/transfers HTTP/1.1
Cookie: JSESSIONID=abc123
Content-Type: application/json

{"accountNumber": "CHK000001", "destinationAccountNumber": "SAV000001", "amount": 500.00}

→ 200 OK: Checking = $4,750.00, Savings = $10,500.00
```

```http
GET /api/transactions HTTP/1.1
Cookie: JSESSIONID=abc123

→ 200 OK: Returns recent transaction history
```

```http
POST /api/withdrawals HTTP/1.1
Cookie: JSESSIONID=abc123
Content-Type: application/json

{"accountNumber": "CHK000001", "amount": 5000.00}

→ 400 OK: {"error": "Insufficient funds", "status": 400}
```

---

*Documentation generated for academic submission. All endpoints are tested with 41 JUnit 5 / Mockito tests covering success paths, validation errors, authorization checks, and transaction rollback scenarios.*
