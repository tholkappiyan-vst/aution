package com.example.aution.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "bid_history",
    indexes = {
        // Primary query: all bids for an auction, ordered by amount desc
        // — used for leaderboard and winner resolution
        @Index(name = "idx_auction_bid_amount", columnList = "auction_id, bidAmount DESC"),

        // Secondary: all bids by a specific bidder across auctions
        @Index(name = "idx_bid_bidder", columnList = "bidder_id"),

        // Time-series queries (analytics, anti-sniper audit trail)
        @Index(name = "idx_bid_placed_at", columnList = "placedAt")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BidHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Core References ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private AuctionDetailsEntity auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private BidderEntity bidder;

    // --- Bid Data (migrated from Redis after Lua script validation) ---
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal bidAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime placedAt;             // Timestamp of the original Redis bid

    @Column(nullable = false, updatable = false)
    private LocalDateTime persistedAt;          // When this record was written to PostgreSQL

    // --- Anti-Sniper Audit ---
    private boolean triggeredExtension;         // Did this bid extend the auction endTime?
    private LocalDateTime newEndTimeIfExtended; // The new endTime if extension was triggered

    // --- Validity flag (for edge cases: e.g. bid placed just as auction closed) ---
    @Builder.Default
    private boolean isValid = true;

    @PrePersist
    protected void onCreate() {
        this.persistedAt = LocalDateTime.now();
    }
}