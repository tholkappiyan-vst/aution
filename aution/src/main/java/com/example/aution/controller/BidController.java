package com.example.aution.controller;

import com.example.aution.dto.bid.BidRequest;
import com.example.aution.dto.bid.BidResponse;
import com.example.aution.dto.bid.BidUpdateMessage;
import com.example.aution.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * BidController exposes:
 *
 *  POST /bids/{auctionId}           → place a bid (JWT required, ROLE_BIDDER)
 *  GET  /bids/{auctionId}/current   → get current price for a bidder joining mid-auction
 *
 * WebSocket flow (handled by STOMP, not HTTP):
 *  Client subscribes to:  /topic/auction/{auctionId}
 *  Client connects to:    ws://localhost:8080/ws (SockJS endpoint)
 *  Server broadcasts to:  /topic/auction/{auctionId} after every accepted bid
 */
@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
@Tag(name = "Bidding", description = "Real-time bid placement and live price feed")
public class BidController {

    private final BidService bidService;

    /**
     * Place a bid on an active auction.
     *
     * The bid goes through the Redis Lua script atomically.
     * If accepted, all connected bidders receive the update via WebSocket.
     * The bidder who placed the bid gets a direct HTTP response.
     *
     * Request body:
     * { "bidAmount": 5500.00 }
     *
     * Possible responses:
     *  200 OK — { "status": "ACCEPTED", "currentHighestBid": 5500.00, "currentLeader": "ravi123" }
     *  200 OK — { "status": "REJECTED", "reason": "Bid too low. Minimum next bid: ₹5100.00" }
     *  200 OK — { "status": "REJECTED", "reason": "This auction is not currently active" }
     *
     * Note: rejected bids still return 200 — rejection is a business outcome,
     * not an HTTP error. The bidder reads the "status" field.
     */
    @PostMapping("/{auctionId}")
    @PreAuthorize("hasRole('BIDDER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Place a bid on an active auction")
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long auctionId,
            @Valid @RequestBody BidRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        BidResponse response = bidService.placeBid(
                auctionId, userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current bid state for a bidder who just connected.
     * Called once when a bidder opens the auction page —
     * after this they receive live updates via WebSocket subscription.
     */
    @GetMapping("/{auctionId}/current")
    @PreAuthorize("hasRole('BIDDER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current highest bid and leader for an auction")
    public ResponseEntity<BidUpdateMessage> getCurrentBidState(
            @PathVariable Long auctionId) {

        return ResponseEntity.ok(bidService.getCurrentBidState(auctionId));
    }
}