package com.example.aution.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "auction_details",
    indexes = {
        // Scheduler queries: WHERE status = 'ACTIVE' AND endTime <= NOW()
        @Index(name = "idx_auction_status",   columnList = "status"),
        @Index(name = "idx_auction_end_time", columnList = "endTime"),
        // Composite for the combined scheduler query — avoids two separate index lookups
        @Index(name = "idx_auction_status_endtime", columnList = "status, endTime")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionDetailsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemDetailsEntity itemDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auctioner_id", nullable = false)
    private AuctionerEntity auctioner;

    // --- Core Financial Fields (BigDecimal prevents floating-point precision loss) ---
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal startingPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal currentHighestBid;

    @Column(precision = 19, scale = 4)
    private BigDecimal reservePrice;        // Minimum the item must reach to be sold

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumIncrement;    // Fixed minimum jump required per bid

    // --- Timeline & Scheduling ---
    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // --- Anti-Sniper Mechanism Settings ---
    @Builder.Default
    private boolean enableAutoExtension = true;     // Protects against last-second snipers

    @Builder.Default
    private Integer extensionWindowMinutes = 2;     // How close to end a bid must be to trigger

    @Builder.Default
    private Integer extensionDurationMinutes = 5;   // Extra time added when sniper bid detected

    // --- System Status (isClosed removed — status = COMPLETED/CANCELLED covers it) ---
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.SCHEDULED;

    // --- Post-Auction Resolution ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_bidder_id")
    private BidderEntity winningBidder;

    private LocalDateTime finalizedAt;
}