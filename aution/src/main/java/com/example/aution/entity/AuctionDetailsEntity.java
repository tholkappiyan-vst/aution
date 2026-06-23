package com.example.aution.entity;




import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "auction_details")
@Getter 

@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
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

    // --- Core Financial Fields ---
    @Column(nullable = false)
    private Double startingPrice;

    private Double currentHighestBid;

    private Double reservePrice; // Minimum price the item must reach to be sold

    @Column(nullable = false)
    private Double minimumIncrement; // The fixed minimum jump required for the next bid

    // --- Timeline & Scheduling ---
    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // --- Anti-Sniper Mechanism Settings ---
    @Builder.Default
    private boolean enableAutoExtension = true; // Protects against last-second snipers
    @Builder.Default
    private Integer extensionWindowMinutes = 2; // How close to the end a bid must be to trigger extension
   
    @Builder.Default
    private Integer extensionDurationMinutes = 5; // How much extra time to add if a sniper bids

    // --- System Status Tracking ---
    @Builder.Default
    private boolean isClosed = false;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private AuctionStatus status = AuctionStatus.SCHEDULED; // TRACKS: SCHEDULED, ACTIVE, PAUSED, COMPLETED, CANCELLED

    // --- Post-Auction Resolution (For Asynchronous DB Processing) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_bidder_id")
    private BidderEntity winningBidder;

    private LocalDateTime finalizedAt;
}

