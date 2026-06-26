package com.example.aution.dto.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error envelope returned for every failed request.
 *
 * Example response for duplicate email:
 * {
 *   "status": 409,
 *   "error": "CONFLICT",
 *   "message": "Email already registered: ravi@example.com",
 *   "path": "/auth/register",
 *   "timestamp": "2026-06-26T12:30:00",
 *   "fieldErrors": null
 * }
 *
 * Example response for validation errors:
 * {
 *   "status": 400,
 *   "error": "VALIDATION_FAILED",
 *   "message": "One or more fields are invalid",
 *   "path": "/auth/register",
 *   "timestamp": "2026-06-26T12:30:00",
 *   "fieldErrors": {
 *     "password": "Password must be at least 8 characters",
 *     "email": "Must be a valid email address"
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // Only populated for field-level validation errors — null otherwise
    private Map<String, String> fieldErrors;
}