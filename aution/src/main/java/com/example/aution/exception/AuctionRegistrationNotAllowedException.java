package com.example.aution.exception;

/**
 * Thrown when a bidder tries to register for an auction
 * that is not SCHEDULED, or within 5 minutes of start.
 * Maps to HTTP 400 BAD_REQUEST.
 */
public class AuctionRegistrationNotAllowedException extends RuntimeException {
    public AuctionRegistrationNotAllowedException(String message) {
        super(message);
    }
}