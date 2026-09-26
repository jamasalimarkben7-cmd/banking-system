package com.bankingsystem.exception;

/**
 * Thrown when a requested resource (account, customer, transaction, user)
 * cannot be located in the database.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}