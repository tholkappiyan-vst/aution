package com.example.aution.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.aution.dto.auction.AuctionResponse;
import com.example.aution.dto.auction.CreateAuctionRequest;
import com.example.aution.entity.AuctionDetailsEntity;
import com.example.aution.entity.AuctionStatus;
import com.example.aution.entity.AuctionerEntity;
import com.example.aution.entity.ItemDetailsEntity;
import com.example.aution.exception.AuctionNotDeletableException;
import com.example.aution.exception.AuctionNotFoundException;
import com.example.aution.repository.AuctionRepository;
import com.example.aution.repository.AuctionerRepository;
import com.example.aution.repository.ItemDetailsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final ItemDetailsRepository itemDetailsRepository;
    private final AuctionerRepository auctionerRepository;

    // ── Create Auction ────────────────────────────────────────────────────────

    /**
     * Creates ItemDetailsEntity + AuctionDetailsEntity in a single transaction.
     * The authenticated auctioneer's username comes from the JWT via SecurityContext.
     * Status starts as SCHEDULED — the @Scheduled job will flip it to ACTIVE.
     */
    @Transactional
    public AuctionResponse createAuction(CreateAuctionRequest request, String username) {

        // Validate time window
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Resolve the auctioneer from the JWT username
        AuctionerEntity auctioner = auctionerRepository
                .findByPersonDetailsUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Auctioneer not found for username: " + username));

        // Step 1: Persist item first (auction FK depends on item id)
        ItemDetailsEntity item = ItemDetailsEntity.builder()
                .name(request.getItemName())
                .description(request.getItemDescription())
                .category(request.getItemCategory())
                .condition(request.getItemCondition())
                .estimatedValue(request.getEstimatedValue())
                .imageUrl(request.getItemImageUrl())
                .secondaryImageUrls(request.getSecondaryImageUrls())
                .upc(request.getUpc())
                .weightKg(request.getWeightKg())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .depthCm(request.getDepthCm())
                .isCertifiedAuthentic(request.isCertifiedAuthentic())
                .certificateNumber(request.getCertificateNumber())
                .provenance(request.getProvenance())
                .build();

        ItemDetailsEntity savedItem = itemDetailsRepository.save(item);

        // Step 2: Build auction with SCHEDULED status
        AuctionDetailsEntity auction = AuctionDetailsEntity.builder()
                .itemDetails(savedItem)
                .auctioner(auctioner)
                .startingPrice(request.getStartingPrice())
                .reservePrice(request.getReservePrice())
                .minimumIncrement(request.getMinimumIncrement())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(AuctionStatus.SCHEDULED)
                // Anti-sniper: use request value or fall back to entity defaults
                .enableAutoExtension(
                        request.getEnableAutoExtension() != null
                        ? request.getEnableAutoExtension() : true)
                .extensionWindowMinutes(
                        request.getExtensionWindowMinutes() != null
                        ? request.getExtensionWindowMinutes() : 2)
                .extensionDurationMinutes(
                        request.getExtensionDurationMinutes() != null
                        ? request.getExtensionDurationMinutes() : 5)
                .build();

        AuctionDetailsEntity saved = auctionRepository.save(auction);

        log.info("Auction created [id={}] by auctioneer [{}] scheduled for {}",
                saved.getId(), username, saved.getStartTime());

        return toResponse(saved);
    }

    // ── Delete Auction (only SCHEDULED, only own) ─────────────────────────────

    /**
     * Auctioneer can only delete their OWN auctions that are still SCHEDULED.
     * Deletes the item too (cascade as per LLD requirement).
     *
     * Ownership is enforced by findByIdAndAuctioner — if the auction exists
     * but belongs to a different auctioneer, it returns empty (same as not found).
     * This prevents leaking whether another auctioneer's auction exists.
     */
    @Transactional
    public void deleteAuction(Long auctionId, String username) {

        AuctionerEntity auctioner = auctionerRepository
                .findByPersonDetailsUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Auctioneer not found for username: " + username));

        // Ownership check — returns empty if auction doesn't belong to this auctioneer
        AuctionDetailsEntity auction = auctionRepository
                .findByIdAndAuctioner(auctionId, auctioner)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        // Business rule: can only delete SCHEDULED auctions
        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new AuctionNotDeletableException(auctionId);
        }

        // Delete auction first (FK constraint), then item (cascade as per LLD)
        Long itemId = auction.getItemDetails().getId();
        auctionRepository.delete(auction);
        itemDetailsRepository.deleteById(itemId);

        log.info("Auction [id={}] and its item [id={}] deleted by auctioneer [{}]",
                auctionId, itemId, username);
    }

    // ── Get My Auctions ───────────────────────────────────────────────────────

    public List<AuctionResponse> getMyAuctions(String username) {
        AuctionerEntity auctioner = auctionerRepository
                .findByPersonDetailsUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Auctioneer not found for username: " + username));

        return auctionRepository.findByAuctioner(auctioner)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get Single Auction (public) ───────────────────────────────────────────

    public AuctionResponse getAuction(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .map(this::toResponse)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));
    }

    // ── Get All Public Auctions (SCHEDULED + ACTIVE) ──────────────────────────

    public List<AuctionResponse> getPublicAuctions() {
        return auctionRepository
                .findByStatusIn(List.of(AuctionStatus.SCHEDULED, AuctionStatus.ACTIVE))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Scheduler: Auto-Start Auctions ───────────────────────────────────────

    /**
     * Runs every 30 seconds. Finds all SCHEDULED auctions whose startTime
     * has passed and flips their status to ACTIVE.
     *
     * fixedDelay = 30000ms — next run starts 30s after the previous one finishes.
     * This prevents overlap if the DB query itself takes time.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void startScheduledAuctions() {
        List<AuctionDetailsEntity> toStart = auctionRepository
                .findByStatusAndStartTimeBefore(AuctionStatus.SCHEDULED, LocalDateTime.now());

        if (!toStart.isEmpty()) {
            toStart.forEach(auction -> {
                auction.setStatus(AuctionStatus.ACTIVE);
                log.info("Auction [id={}] automatically started at {}", 
                        auction.getId(), LocalDateTime.now());
            });
            auctionRepository.saveAll(toStart);
        }
    }

    // ── Scheduler: Auto-Complete Auctions ─────────────────────────────────────

    /**
     * Runs every 30 seconds. Finds all ACTIVE auctions whose endTime
     * has passed and flips their status to COMPLETED.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void completeExpiredAuctions() {
        List<AuctionDetailsEntity> toComplete = auctionRepository
                .findByStatusAndEndTimeBefore(AuctionStatus.ACTIVE, LocalDateTime.now());

        if (!toComplete.isEmpty()) {
            toComplete.forEach(auction -> {
                auction.setStatus(AuctionStatus.COMPLETED);
                auction.setFinalizedAt(LocalDateTime.now());
                log.info("Auction [id={}] automatically completed at {}",
                        auction.getId(), LocalDateTime.now());
            });
            auctionRepository.saveAll(toComplete);
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private AuctionResponse toResponse(AuctionDetailsEntity a) {
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
                // Auctioneer
                .auctioneerId(auctioner.getId())
                .auctioneerUsername(auctioner.getPersonDetails().getUsername())
                .companyName(auctioner.getCompanyName())
                // Item
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