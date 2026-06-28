package com.example.aution.exception;

/**
 * Thrown when a bidder tries to unregister from an auction
 * they were never registered for.
 * Maps to HTTP 404 NOT_FOUND.
 */
public class RegistrationNotFoundException extends RuntimeException {
    public RegistrationNotFoundException(Long auctionId) {
        super("No registration found for auction: " + auctionId);
    }
}