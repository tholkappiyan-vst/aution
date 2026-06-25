package com.example.aution.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "auction_registrations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"auction_id", "bidder_id"})
    },
    indexes = {
        @Index(name = "idx_reg_auction",  columnList = "auction_id"),
        @Index(name = "idx_reg_bidder",   columnList = "bidder_id"),
        @Index(name = "idx_reg_approved", columnList = "isApproved")
    }
)
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

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    // --- Approval (audit trail: who approved, when) ---
    @Builder.Default
    private boolean isApproved = true;          // Auto-approved by default

    private LocalDateTime approvedAt;           // Null until explicitly approved

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_admin_id")  // Null if auto-approved
    private AdminEntity approvedBy;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
        // If auto-approved on creation, stamp the approval time too
        if (this.isApproved) {
            this.approvedAt = LocalDateTime.now();
        }
    }
}