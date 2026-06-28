package com.example.aution.dto.bid;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Broadcasted to every bidder subscribed to /topic/auction/{id}
 * after every accepted bid.
 *
 * The bidder's frontend receives this and updates the live price display.
 *
 * Example payload:
 * {
 *   "auctionId": 5,
 *   "currentHighestBid": 5500.00,
 *   "currentLeader": "ravi123",
 *   "bidPlacedAt": "2026-06-26T15:30:00.123",
 *   "message": "ravi123 placed ₹5500.00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdateMessage {
    private Long auctionId;
    private BigDecimal currentHighestBid;
    private String currentLeader;          // username of the highest bidder
    private LocalDateTime bidPlacedAt;
    private String message;                // Human-readable update for UI display
}