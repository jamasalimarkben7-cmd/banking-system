package com.bankingsystem.dto.api;

import java.time.Instant;
import java.util.List;

/**
 * Standardized JSON error envelope returned by the REST API.
 */
public class ApiError {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Instant timestamp;
    private final List<String> details;

    public ApiError(int status, String error, String message, String path,
                    List<String> details) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
        this.details = details;
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public Instant getTimestamp() { return timestamp; }
    public List<String> getDetails() { return details; }
}