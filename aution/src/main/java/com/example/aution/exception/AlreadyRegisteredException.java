package com.example.aution.exception;

/**
 * Thrown when a bidder tries to register for an auction
 * they are already registered for.
 * Maps to HTTP 409 CONFLICT.
 */
public class AlreadyRegisteredException extends RuntimeException {
    public AlreadyRegisteredException(Long auctionId) {
        super("You are already registered for auction: " + auctionId);
    }
}