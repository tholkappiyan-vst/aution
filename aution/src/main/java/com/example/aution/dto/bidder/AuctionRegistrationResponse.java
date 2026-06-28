package com.example.aution.dto.bidder;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned after a bidder registers or unregisters from an auction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionRegistrationResponse {

    private Long registrationId;
    private Long auctionId;
    private String itemName;
    private String auctioneerCompany;
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;
    private boolean approved;
    private LocalDateTime registeredAt;
    private String message;             // Human-readable outcome e.g. "Successfully registered"
}