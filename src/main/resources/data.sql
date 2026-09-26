-- Database initialization script for Banking System
-- Compatible with both H2 and PostgreSQL

-- Create customer user with BCrypt-hashed password
-- Password: "password123"
-- BCrypt hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

-- H2 uses MERGE INTO, PostgreSQL uses ON CONFLICT
-- This script uses standard INSERT with WHERE NOT EXISTS for compatibility

-- Insert user (for authentication)
INSERT INTO users (username, password, role, email, first_name, last_name)
SELECT 'customer', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'CLIENT', 'customer@example.com', 'John', 'Doe'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'customer');

-- Insert customer (for banking data)
INSERT INTO customers (username, email, first_name, last_name, customer_type)
SELECT 'customer', 'customer@example.com', 'John', 'Doe', 'INDIVIDUAL'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE username = 'customer');

-- Insert admin customer (matches admin User created by CommandLineRunner)
INSERT INTO customers (username, email, first_name, last_name, customer_type)
SELECT 'admin', 'admin@example.com', 'Admin', 'User', 'INDIVIDUAL'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE username = 'admin');

-- Admin user is created by CommandLineRunner in BankingSystemApplication.java
-- with username 'admin' and password 'admin123'

-- Create sample accounts for customer (linked to customer_id)
INSERT INTO accounts (account_number, balance, account_type, customer_id)
SELECT 'CHK000001', 5000.00, 'CHECKING', c.id
FROM customers c WHERE c.username = 'customer'
  AND NOT EXISTS (SELECT 1 FROM accounts WHERE account_number = 'CHK000001');

INSERT INTO accounts (account_number, balance, account_type, customer_id)
SELECT 'SAV000001', 10000.00, 'SAVINGS', c.id
FROM customers c WHERE c.username = 'customer'
  AND NOT EXISTS (SELECT 1 FROM accounts WHERE account_number = 'SAV000001');

-- Create sample account for admin (will be linked after admin customer is created by CommandLineRunner)
-- This will run on subsequent startups when admin customer exists
INSERT INTO accounts (account_number, balance, account_type, customer_id)
SELECT 'ADM000001', 100000.00, 'CHECKING', c.id
FROM customers c WHERE c.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM accounts WHERE account_number = 'ADM000001');

-- Create additional sample accounts for admin customer
INSERT INTO accounts (account_number, balance, account_type, customer_id)
SELECT 'ADMCHK0001', 5000.00, 'CHECKING', c.id
FROM customers c WHERE c.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM accounts WHERE account_number = 'ADMCHK0001');

INSERT INTO accounts (account_number, balance, account_type, customer_id)
SELECT 'ADMSAV0001', 10000.00, 'SAVINGS', c.id
FROM customers c WHERE c.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM accounts WHERE account_number = 'ADMSAV0001');