package com.example.aution.dto.bid;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Returned directly to the bidder who placed the bid via HTTP response.
 * All OTHER bidders receive BidUpdateMessage via WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private String status;                  // "ACCEPTED", "REJECTED"
    private String reason;                  // null if accepted, reason if rejected
    private Long auctionId;
    private BigDecimal yourBidAmount;
    private BigDecimal currentHighestBid;   // reflects your bid if accepted
    private String currentLeader;
    private LocalDateTime placedAt;
}