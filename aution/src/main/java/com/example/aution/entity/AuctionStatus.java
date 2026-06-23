package com.example.aution.entity;


    public enum AuctionStatus {
    SCHEDULED, // Auction is created but waiting for startTime
    ACTIVE,    // Bidding is currently open and live in Redis
    PAUSED,    // Temporarily halted by an Admin or Auctioneer
    COMPLETED, // Ended normally with a winner or failed to meet reserve
    CANCELLED  // Aborted before or during execution
}


