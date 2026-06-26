package com.example.aution.exception;

/**
 * Thrown when a registration attempt uses an already-taken username or email.
 * Maps to HTTP 409 CONFLICT.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}