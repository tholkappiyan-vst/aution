package com.example.aution.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_registrations", uniqueConstraints = {
    // Ensures a bidder cannot register for the same auction multiple times
    @UniqueConstraint(columnNames = {"auction_id", "bidder_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionRegistrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private AuctionDetailsEntity auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private BidderEntity bidder;

    private LocalDateTime registeredAt;

    @Builder.Default
    private boolean isApproved = true; // Auto-approved by default, can be false if admins need to vet balances

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
    }
}
