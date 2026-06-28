package com.example.aution.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.aution.dto.bidder.AuctionRegistrationResponse;
import com.example.aution.service.BidderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * BidderController exposes:
 *
 * PUBLIC (no JWT needed):
 *   GET  /auctions                    → list all available auctions
 *   GET  /auctions/{id}               → get auction + item details
 *
 * BIDDER only (JWT required, ROLE_BIDDER):
 *   POST   /bidder/auctions/{id}/register    → register for an auction
 *   DELETE /bidder/auctions/{id}/unregister  → unregister from an auction
 *   GET    /bidder/registrations             → view my registered auctions
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Bidder", description = "Bidder auction browsing and registration")
public class BidderController {

    private final BidderService bidderService;

    
   

    // ── Bidder Protected Endpoints ────────────────────────────────────────────

    /**
     * Register for a SCHEDULED auction.
     * Rules enforced in service:
     *  - Auction must be SCHEDULED
     *  - Must be at least 5 minutes before startTime
     *  - Cannot register twice
     */
    @PostMapping("/bidder/auctions/{id}/register")
    @PreAuthorize("hasRole('BIDDER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Register for a scheduled auction (min 5 mins before start)")
    public ResponseEntity<AuctionRegistrationResponse> registerForAuction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        AuctionRegistrationResponse response =
                bidderService.registerForAuction(id, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Unregister from a SCHEDULED auction.
     * Returns 204 No Content on success — nothing to return after deletion.
     */
    @DeleteMapping("/bidder/auctions/{id}/unregister")
    @PreAuthorize("hasRole('BIDDER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Unregister from a scheduled auction")
    public ResponseEntity<Void> unregisterFromAuction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        bidderService.unregisterFromAuction(id, userDetails.getUsername());
        return ResponseEntity.noContent().build(); // 204
    }

    /**
     * View all auctions the logged-in bidder has registered for.
     */
    @GetMapping("/bidder/registrations")
    @PreAuthorize("hasRole('BIDDER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "View all my auction registrations")
    public ResponseEntity<List<AuctionRegistrationResponse>> getMyRegistrations(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                bidderService.getMyRegistrations(userDetails.getUsername()));
    }
}