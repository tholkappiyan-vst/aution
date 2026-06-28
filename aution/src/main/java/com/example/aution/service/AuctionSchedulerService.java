package com.example.aution.service;

import com.example.aution.entity.AuctionDetailsEntity;
import com.example.aution.entity.AuctionStatus;
import com.example.aution.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuctionSchedulerService runs two @Scheduled jobs:
 *
 *  startScheduledAuctions()    — flips SCHEDULED → ACTIVE, seeds Redis
 *  completeExpiredAuctions()   — flips ACTIVE → COMPLETED, finalizes bids
 *
 * Both run every 30 seconds (fixedDelay).
 * @EnableScheduling must be on your main Application class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSchedulerService {

    private final AuctionRepository auctionRepository;
    private final BidService bidService;
    private final BidPersistenceService bidPersistenceService;

    // ── Auto-Start ────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void startScheduledAuctions() {
        List<AuctionDetailsEntity> toStart = auctionRepository
                .findByStatusAndStartTimeBefore(
                        AuctionStatus.SCHEDULED, LocalDateTime.now());

        if (toStart.isEmpty()) return;

        for (AuctionDetailsEntity auction : toStart) {
            auction.setStatus(AuctionStatus.ACTIVE);

            // Seed Redis keys + register Pub/Sub listener
            // From this moment, bids can flow through the Lua script
            bidService.seedAuction(auction);

            log.info("Auction [id={}] started — Redis seeded, accepting bids",
                    auction.getId());
        }

        auctionRepository.saveAll(toStart);
    }

    // ── Auto-Complete ─────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void completeExpiredAuctions() {
        List<AuctionDetailsEntity> toComplete = auctionRepository
                .findByStatusAndEndTimeBefore(
                        AuctionStatus.ACTIVE, LocalDateTime.now());

        if (toComplete.isEmpty()) return;

        for (AuctionDetailsEntity auction : toComplete) {
            // Mark INACTIVE in Redis first — stops new bids immediately
            bidService.completeAuction(auction.getId());

            // Drain remaining bid queue, set winner, clean Redis keys
            bidPersistenceService.finalDrain(auction);

            // Flip DB status to COMPLETED
            auction.setStatus(AuctionStatus.COMPLETED);

            log.info("Auction [id={}] completed and finalized", auction.getId());
        }

        auctionRepository.saveAll(toComplete);
    }
}