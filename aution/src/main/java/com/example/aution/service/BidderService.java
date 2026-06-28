package com.example.aution.service;

import com.example.aution.dto.auction.AuctionResponse;
import com.example.aution.dto.bidder.AuctionRegistrationResponse;
import com.example.aution.entity.*;
import com.example.aution.exception.*;
import com.example.aution.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidderService {

    private final BidderRepository bidderRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionRegistrationRepository registrationRepository;

    // ── 1. View all SCHEDULED + ACTIVE auctions ───────────────────────────────

    /**
     * Reuses AuctionRepository — returns SCHEDULED and ACTIVE auctions.
     * Public endpoint but bidder can also call this while logged in.
     */
    public List<AuctionResponse> getAllAvailableAuctions() {
        return auctionRepository
                .findByStatusIn(List.of(AuctionStatus.SCHEDULED, AuctionStatus.ACTIVE))
                .stream()
                .map(this::toAuctionResponse)
                .collect(Collectors.toList());
    }

    // ── 2. View item details of a particular auction ──────────────────────────

    public AuctionResponse getAuctionDetails(Long auctionId) {
        AuctionDetailsEntity auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));
        return toAuctionResponse(auction);
    }

    // ── 3. Register for an auction ────────────────────────────────────────────

    /**
     * Business rules enforced in order:
     *  1. Auction must exist
     *  2. Auction must be SCHEDULED (not ACTIVE, COMPLETED, CANCELLED)
     *  3. Registration must be at least 5 minutes before startTime
     *  4. Bidder must not already be registered (duplicate guard)
     *
     * All pass → auto-approved registration created and returned.
     */
    @Transactional
    public AuctionRegistrationResponse registerForAuction(Long auctionId, String username) {

        BidderEntity bidder = resolveBidder(username);

        AuctionDetailsEntity auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        // Rule 1: Only SCHEDULED auctions accept new registrations
        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new AuctionRegistrationNotAllowedException(
                "Registration is only allowed for SCHEDULED auctions. " +
                "This auction is currently: " + auction.getStatus()
            );
        }

        // Rule 2: Must register at least 5 minutes before startTime
        LocalDateTime cutoff = auction.getStartTime().minusMinutes(5);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new AuctionRegistrationNotAllowedException(
                "Registration closed. You must register at least 5 minutes " +
                "before the auction starts (" + auction.getStartTime() + ")"
            );
        }

        // Rule 3: Duplicate registration guard
        if (registrationRepository.existsByAuctionAndBidder(auction, bidder)) {
            throw new AlreadyRegisteredException(auctionId);
        }

        // All rules passed — create auto-approved registration
        AuctionRegistrationEntity registration = AuctionRegistrationEntity.builder()
                .auction(auction)
                .bidder(bidder)
                .isApproved(true)
                .approvedAt(LocalDateTime.now())   // auto-approved immediately
                .build();
        // registeredAt is stamped by @PrePersist in the entity

        AuctionRegistrationEntity saved = registrationRepository.save(registration);

        log.info("Bidder [{}] registered for auction [id={}]", username, auctionId);

        return toRegistrationResponse(saved, "Successfully registered for auction");
    }

    // ── 4. Unregister from an auction ─────────────────────────────────────────

    /**
     * Business rules:
     *  1. Auction must exist
     *  2. Registration must exist for this bidder + auction
     *  3. Auction must still be SCHEDULED — cannot unregister once it has started
     */
    @Transactional
    public void unregisterFromAuction(Long auctionId, String username) {

        BidderEntity bidder = resolveBidder(username);

        AuctionDetailsEntity auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        // Rule: cannot unregister after auction has started
        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new AuctionRegistrationNotAllowedException(
                "You cannot unregister from an auction that has already started."
            );
        }

        // Find the specific registration record
        AuctionRegistrationEntity registration = registrationRepository
                .findByAuctionAndBidder(auction, bidder)
                .orElseThrow(() -> new RegistrationNotFoundException(auctionId));

        registrationRepository.delete(registration);

        log.info("Bidder [{}] unregistered from auction [id={}]", username, auctionId);
    }

    // ── 5. View my registered auctions ───────────────────────────────────────

    public List<AuctionRegistrationResponse> getMyRegistrations(String username) {
        BidderEntity bidder = resolveBidder(username);

        return registrationRepository.findByBidder(bidder)
                .stream()
                .map(r -> toRegistrationResponse(r, null))
                .collect(Collectors.toList());
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private BidderEntity resolveBidder(String username) {
        return bidderRepository.findByPersonDetailsUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Bidder not found for username: " + username));
    }

    private AuctionRegistrationResponse toRegistrationResponse(
            AuctionRegistrationEntity r, String message) {

        AuctionDetailsEntity auction = r.getAuction();
        return AuctionRegistrationResponse.builder()
                .registrationId(r.getId())
                .auctionId(auction.getId())
                .itemName(auction.getItemDetails().getName())
                .auctioneerCompany(auction.getAuctioner().getCompanyName())
                .auctionStartTime(auction.getStartTime())
                .auctionEndTime(auction.getEndTime())
                .approved(r.isApproved())
                .registeredAt(r.getRegisteredAt())
                .message(message)
                .build();
    }

    private AuctionResponse toAuctionResponse(AuctionDetailsEntity a) {
        ItemDetailsEntity item = a.getItemDetails();
        AuctionerEntity auctioner = a.getAuctioner();

        return AuctionResponse.builder()
                .auctionId(a.getId())
                .status(a.getStatus())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .startingPrice(a.getStartingPrice())
                .currentHighestBid(a.getCurrentHighestBid())
                .reservePrice(a.getReservePrice())
                .minimumIncrement(a.getMinimumIncrement())
                .enableAutoExtension(a.isEnableAutoExtension())
                .extensionWindowMinutes(a.getExtensionWindowMinutes())
                .extensionDurationMinutes(a.getExtensionDurationMinutes())
                .auctioneerId(auctioner.getId())
                .auctioneerUsername(auctioner.getPersonDetails().getUsername())
                .companyName(auctioner.getCompanyName())
                .itemId(item.getId())
                .itemName(item.getName())
                .itemDescription(item.getDescription())
                .itemCategory(item.getCategory())
                .itemCondition(item.getCondition())
                .estimatedValue(item.getEstimatedValue())
                .itemImageUrl(item.getImageUrl())
                .secondaryImageUrls(item.getSecondaryImageUrls())
                .certifiedAuthentic(item.isCertifiedAuthentic())
                .certificateNumber(item.getCertificateNumber())
                .build();
    }
}