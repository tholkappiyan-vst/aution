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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.RedisTemplate;

import com.example.aution.dto.auction.AuctionResponse;
import com.example.aution.dto.auction.CreateAuctionRequest;
import com.example.aution.service.AuctionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AuctionController exposes:
 *
 * PUBLIC  (no JWT):
 *   GET  /auctions          → list all SCHEDULED + ACTIVE auctions
 *   GET  /auctions/{id}     → get a single auction detail
 *
 * AUCTIONEER only (JWT required, ROLE_AUCTIONEER):
 *   POST   /auctioneer/auctions         → create auction + item
 *   GET    /auctioneer/auctions/my      → list own auctions
 *   DELETE /auctioneer/auctions/{id}    → delete own SCHEDULED auction
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Auctions", description = "Auction creation, listing, and management")
public class AuctionController {

    private final AuctionService auctionService;
    private final RedisTemplate<String, String> redisTemplate;

    // ── Public Endpoints (no JWT) ─────────────────────────────────────────────

    @GetMapping("/auctions")
    @Operation(summary = "List all active and scheduled auctions (public)")
    public ResponseEntity<List<AuctionResponse>> getPublicAuctions() {
        return ResponseEntity.ok(auctionService.getPublicAuctions());
    }

    @GetMapping("/auctions/{id}")
    @Operation(summary = "Get a single auction by ID (public)")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuction(id));
    }

    // ── Auctioneer Endpoints (JWT required) ───────────────────────────────────

    /**
     * Creates auction + item in one request.
     * @AuthenticationPrincipal extracts the logged-in auctioneer's username
     * from the JWT without any extra DB call.
     */
    @PostMapping("/auctioneer/auctions")
    @PreAuthorize("hasRole('AUCTIONEER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new auction with item details")
    public ResponseEntity<AuctionResponse> createAuction(
            @Valid @RequestBody CreateAuctionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        AuctionResponse response = auctionService.createAuction(
                request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/auctioneer/auctions/my")
    @PreAuthorize("hasRole('AUCTIONEER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all auctions created by the logged-in auctioneer")
    public ResponseEntity<List<AuctionResponse>> getMyAuctions(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                auctionService.getMyAuctions(userDetails.getUsername()));
    }

    @DeleteMapping("/auctioneer/auctions/{id}")
    @PreAuthorize("hasRole('AUCTIONEER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete own SCHEDULED auction (cascades to item)")
    public ResponseEntity<Void> deleteAuction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        auctionService.deleteAuction(id, userDetails.getUsername());
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/redis/ping")
public ResponseEntity<String> ping() {
    redisTemplate.opsForValue().set("ping", "pong");
    String result = redisTemplate.opsForValue().get("ping");
    return ResponseEntity.ok("Redis says: " + result);
}
}