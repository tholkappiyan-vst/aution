package com.example.aution.exception;

/**
 * Thrown when an auction is not found or doesn't belong to the requesting auctioneer.
 * Maps to HTTP 404 via GlobalExceptionHandler.
 */
public class AuctionNotFoundException extends RuntimeException {
    public AuctionNotFoundException(Long auctionId) {
        super("Auction not found with id: " + auctionId);
    }
}