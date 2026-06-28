package com.example.aution.service;

import com.example.aution.entity.*;
import com.example.aution.repository.AuctionRepository;
import com.example.aution.repository.BidHistoryRepository;
import com.example.aution.repository.BidderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * BidPersistenceService drains the Redis write-behind queue into PostgreSQL.
 *
 * Why write-behind?
 * During peak bidding (last 30 seconds of an auction), thousands of bids
 * can arrive per second. Writing each one directly to PostgreSQL would:
 *  - Create row-level lock contention
 *  - Spike DB CPU and connection pool
 *  - Potentially crash the DB under load
 *
 * Instead, the Lua script pushes each validated bid to a Redis List (a queue).
 * This job drains that queue every 5 seconds in a single batch transaction.
 * PostgreSQL sees a steady batch write instead of a spike.
 *
 * The trade-off: bids appear in PostgreSQL up to 5 seconds after placement.
 * This is acceptable because Redis is the source of truth during the auction.
 * PostgreSQL is for analytics, auditing, and winner resolution after completion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BidPersistenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private final BidHistoryRepository bidHistoryRepository;
    private final AuctionRepository auctionRepository;
    private final BidderRepository bidderRepository;

    private static final int DRAIN_BATCH_SIZE = 100; // max bids per drain cycle

    /**
     * Runs every 5 seconds.
     * Pops up to DRAIN_BATCH_SIZE entries from every active auction's bid queue
     * and writes them to BidHistoryEntity in a single transaction.
     *
     * Queue entry format: "bidAmount:bidderUsername:timestampMillis"
     * e.g. "5500.00:ravi123:1719398400000"
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void drainBidQueues() {
        // Find all ACTIVE auctions from DB to know which queues exist
        List<AuctionDetailsEntity> activeAuctions = auctionRepository
                .findByStatusAndStartTimeBefore(AuctionStatus.ACTIVE,
                        LocalDateTime.now().plusYears(1));

        if (activeAuctions.isEmpty()) return;

        List<BidHistoryEntity> batch = new ArrayList<>();

        for (AuctionDetailsEntity auction : activeAuctions) {
            String queueKey = BidService.keyQueue(auction.getId());

            // RPOP drains from the tail (oldest entries first — FIFO order)
            for (int i = 0; i < DRAIN_BATCH_SIZE; i++) {
                String entry = redisTemplate.opsForList().rightPop(queueKey);
                if (entry == null) break; // queue empty for this auction

                BidHistoryEntity bidRecord = parseBidEntry(entry, auction);
                if (bidRecord != null) batch.add(bidRecord);
            }
        }

        if (!batch.isEmpty()) {
            bidHistoryRepository.saveAll(batch);
            log.info("Drained {} bids from Redis queues to PostgreSQL", batch.size());
        }
    }

    /**
     * Final drain after auction completes — empties remaining queue entries
     * and sets the winning bidder on AuctionDetailsEntity.
     *
     * Called by AuctionSchedulerService after flipping status to COMPLETED.
     */
    @Transactional
    public void finalDrain(AuctionDetailsEntity auction) {
        String queueKey = BidService.keyQueue(auction.getId());
        String highestBidKey = BidService.keyHighestBid(auction.getId());
        String leaderKey = BidService.keyLeader(auction.getId());

        // Drain remaining bids
        List<BidHistoryEntity> remaining = new ArrayList<>();
        String entry;
        while ((entry = redisTemplate.opsForList().rightPop(queueKey)) != null) {
            BidHistoryEntity record = parseBidEntry(entry, auction);
            if (record != null) remaining.add(record);
        }
        if (!remaining.isEmpty()) bidHistoryRepository.saveAll(remaining);

        // Set winning bidder on the auction
        String winnerUsername = redisTemplate.opsForValue().get(leaderKey);
        String highestBid = redisTemplate.opsForValue().get(highestBidKey);

        if (winnerUsername != null && !winnerUsername.isEmpty()) {
            bidderRepository.findByPersonDetailsUsername(winnerUsername).ifPresent(winner -> {
                auction.setWinningBidder(winner);
                auction.setCurrentHighestBid(new BigDecimal(highestBid));
                auction.setFinalizedAt(LocalDateTime.now());
                auctionRepository.save(auction);
                log.info("Auction [id={}] won by [{}] at ₹{}",
                        auction.getId(), winnerUsername, highestBid);
            });
        } else {
            // No bids placed — reserve not met
            auction.setFinalizedAt(LocalDateTime.now());
            auctionRepository.save(auction);
            log.info("Auction [id={}] completed with no bids", auction.getId());
        }

        // Clean up Redis keys
        redisTemplate.delete(List.of(
                BidService.keyStatus(auction.getId()),
                BidService.keyHighestBid(auction.getId()),
                BidService.keyLeader(auction.getId()),
                BidService.keyIncrement(auction.getId()),
                BidService.keyQueue(auction.getId()),
                BidService.keyPubSub(auction.getId())
        ));

        log.info("Redis keys cleaned up for auction [id={}]", auction.getId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BidHistoryEntity parseBidEntry(
            String entry, AuctionDetailsEntity auction) {
        try {
            String[] parts = entry.split(":", 3);
            if (parts.length != 3) {
                log.warn("Malformed bid queue entry: {}", entry);
                return null;
            }

            BigDecimal amount = new BigDecimal(parts[0]);
            String username = parts[1];
            long timestamp = Long.parseLong(parts[2]);

            LocalDateTime placedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

            return bidderRepository.findByPersonDetailsUsername(username)
                    .map(bidder -> BidHistoryEntity.builder()
                            .auction(auction)
                            .bidder(bidder)
                            .bidAmount(amount)
                            .placedAt(placedAt)
                            .isValid(true)
                            .build())
                    .orElse(null);

        } catch (Exception e) {
            log.error("Failed to parse bid entry [{}]: {}", entry, e.getMessage());
            return null;
        }
    }
}