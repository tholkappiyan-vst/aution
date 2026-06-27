package com.example.aution.exception;

/**
 * Thrown when auctioneer tries to delete an auction that is already ACTIVE/COMPLETED.
 * Maps to HTTP 409 via GlobalExceptionHandler.
 */
public class AuctionNotDeletableException extends RuntimeException {
    public AuctionNotDeletableException(Long auctionId) {
        super("Auction " + auctionId + " cannot be deleted because it has already started.");
    }
}