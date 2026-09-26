package com.bankingsystem.dto.api;

import java.time.Instant;

/**
 * Standardized JSON success envelope returned by the REST API.
 */
public class SuccessResponse {

    private final int status;
    private final String message;
    private final Object data;
    private final Instant timestamp;

    public SuccessResponse(int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public Instant getTimestamp() { return timestamp; }
}