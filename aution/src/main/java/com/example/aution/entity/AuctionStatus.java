package com.example.aution.entity;

public enum AuctionStatus {
    SCHEDULED,  // Created but waiting for startTime
    ACTIVE,     // Bidding is live — state also held in Redis
    PAUSED,     // Temporarily halted by Admin or Auctioneer for triage
    COMPLETED,  // Ended normally — covers both SOLD (reserve met) and
                // NO_SALE (reserve not met). Check winningBidder != null to distinguish.
    CANCELLED   // Aborted before or during execution
}