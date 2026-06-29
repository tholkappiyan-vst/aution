package com.example.aution.service;

import com.example.aution.dto.bid.BidRequest;
import com.example.aution.dto.bid.BidResponse;
import com.example.aution.dto.bid.BidUpdateMessage;
import com.example.aution.entity.AuctionDetailsEntity;
import com.example.aution.entity.BidderEntity;
import com.example.aution.exception.AuctionNotFoundException;
import com.example.aution.listener.BidUpdateListener;
import com.example.aution.repository.AuctionRegistrationRepository;
import com.example.aution.repository.AuctionRepository;
import com.example.aution.repository.BidderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BidService handles:
 *
 *  placeBid()      — runs the Lua script atomically, returns result to bidder
 *  seedAuction()   — called when auction goes ACTIVE: seeds Redis keys + registers listener
 *  getCurrentBid() — reads current price from Redis for any connected bidder
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<String> bidLuaScript;
    private final AuctionRepository auctionRepository;
    private final BidderRepository bidderRepository;
    private final AuctionRegistrationRepository registrationRepository;
    private final RedisMessageListenerContainer listenerContainer;
    private final ApplicationContext applicationContext; // to get fresh BidUpdateListener beans

    // ── Redis key helpers ────────────────────────────────────────────────────

    public static String keyStatus(Long id)      { return "auction:" + id + ":status"; }
    public static String keyHighestBid(Long id)  { return "auction:" + id + ":highestBid"; }
    public static String keyLeader(Long id)      { return "auction:" + id + ":highestBidder"; }
    public static String keyIncrement(Long id)   { return "auction:" + id + ":minIncrement"; }
    public static String keyQueue(Long id)       { return "auction:" + id + ":bidQueue"; }
    public static String keyPubSub(Long id)      { return "auction:" + id + ":pubsub"; }

    // ── Place a bid ──────────────────────────────────────────────────────────

    /**
     * Executes the Lua bid validation script atomically in Redis.
     *
     * Guards BEFORE hitting Redis:
     *  1. Bidder must be registered for this auction
     *  2. Auction must exist in DB
     *
     * Then Redis Lua handles:
     *  3. Auction must be ACTIVE in Redis
     *  4. Bid must be > currentHighestBid + minimumIncrement
     *
     * Returns BidResponse with status ACCEPTED or REJECTED + reason.
     */
    public BidResponse placeBid(Long auctionId, String username, BidRequest request) {

        // Guard 1: Bidder must exist
        BidderEntity bidder = bidderRepository.findByPersonDetailsUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Bidder not found: " + username));

        // Guard 2: Auction must exist
        AuctionDetailsEntity auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        // Guard 3: Bidder must be registered for this auction
        boolean registered = registrationRepository
                .existsByAuctionAndBidder(auction, bidder);
        if (!registered) {
            return BidResponse.builder()
                    .status("REJECTED")
                    .reason("You must register for this auction before bidding")
                    .auctionId(auctionId)
                    .yourBidAmount(request.getBidAmount())
                    .placedAt(LocalDateTime.now())
                    .build();
        }

        // Execute Lua script atomically
        // KEYS order must match RedisConfig.bidLuaScript() documentation
        List<String> keys = List.of(
                keyStatus(auctionId),
                keyHighestBid(auctionId),
                keyLeader(auctionId),
                keyIncrement(auctionId),
                keyQueue(auctionId),
                keyPubSub(auctionId)
        );

        String result = redisTemplate.execute(
                bidLuaScript,
                keys,
                request.getBidAmount().toPlainString(),
                username,
                String.valueOf(System.currentTimeMillis())
        );

        return buildResponse(auctionId, username, request.getBidAmount(), result);
    }

    // ── Seed auction in Redis (called when auction goes ACTIVE) ──────────────

    /**
     * Seeds all Redis keys for an auction when it transitions to ACTIVE.
     * Also registers a BidUpdateListener for the auction's Pub/Sub channel.
     *
     * Called by AuctionSchedulerService.startScheduledAuctions().
     *
     * Redis keys seeded:
     *  status       = "ACTIVE"
     *  highestBid   = startingPrice (initial price floor)
     *  highestBidder = "" (no leader yet)
     *  minIncrement = minimumIncrement
     *
     * Keys have no TTL — they are cleaned up by completeAuction().
     */
    public void seedAuction(AuctionDetailsEntity auction) {
        Long id = auction.getId();

        redisTemplate.opsForValue().set(keyStatus(id), "ACTIVE");
        redisTemplate.opsForValue().set(
                keyHighestBid(id),
                auction.getStartingPrice().toPlainString());
        redisTemplate.opsForValue().set(keyLeader(id), "");
        redisTemplate.opsForValue().set(
                keyIncrement(id),
                auction.getMinimumIncrement().toPlainString());

        // Register Pub/Sub listener for this auction's channel
        BidUpdateListener listener = applicationContext.getBean(BidUpdateListener.class);
        listener.setAuctionId(id);
        listenerContainer.addMessageListener(
                listener,
                new ChannelTopic(keyPubSub(id)));

        log.info("Auction [id={}] seeded in Redis and listener registered", id);
    }

    // ── Complete auction in Redis (called when auction ends) ─────────────────

    /**
     * Marks auction as INACTIVE in Redis to stop accepting bids.
     * Called by AuctionSchedulerService.completeExpiredAuctions().
     *
     * Actual DB finalization (winningBidder, finalizedAt) is handled
     * by the write-behind drain job after all queued bids are persisted.
     */
    public void completeAuction(Long auctionId) {
        redisTemplate.opsForValue().set(keyStatus(auctionId), "COMPLETED");
        log.info("Auction [id={}] marked COMPLETED in Redis", auctionId);
    }

    // ── Get current bid state (for bidder joining mid-auction) ───────────────

    /**
     * Returns the current highest bid + leader from Redis.
     * Called when a bidder first connects to the WebSocket and needs
     * the current state before the next bid update arrives.
     */
    public BidUpdateMessage getCurrentBidState(Long auctionId) {
        String bid = redisTemplate.opsForValue().get(keyHighestBid(auctionId));
        String leader = redisTemplate.opsForValue().get(keyLeader(auctionId));

        return BidUpdateMessage.builder()
                .auctionId(auctionId)
                .currentHighestBid(bid != null ? new BigDecimal(bid) : BigDecimal.ZERO)
                .currentLeader(leader != null ? leader : "No bids yet")
                .bidPlacedAt(LocalDateTime.now())
                .message(leader != null && !leader.isEmpty()
                        ? "Current leader: " + leader
                        : "No bids placed yet")
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BidResponse buildResponse(
            Long auctionId, String username,
            BigDecimal bidAmount, String luaResult) {

        return switch (luaResult) {
            case "OK" -> {
                // Read back the current state to confirm
                String currentBid = redisTemplate.opsForValue()
                        .get(keyHighestBid(auctionId));
                String leader = redisTemplate.opsForValue()
                        .get(keyLeader(auctionId));

                yield BidResponse.builder()
                        .status("ACCEPTED")
                        .auctionId(auctionId)
                        .yourBidAmount(bidAmount)
                        .currentHighestBid(new BigDecimal(currentBid))
                        .currentLeader(leader)
                        .placedAt(LocalDateTime.now())
                        .build();
            }
            case "AUCTION_INACTIVE" -> BidResponse.builder()
                    .status("REJECTED")
                    .reason("This auction is not currently active")
                    .auctionId(auctionId)
                    .yourBidAmount(bidAmount)
                    .currentLeader("Auction not active")    // ← add this
                    .placedAt(LocalDateTime.now())
                    .build();

            case "BID_TOO_LOW" -> {
                String currentBid = redisTemplate.opsForValue()
                        .get(keyHighestBid(auctionId));
                String increment = redisTemplate.opsForValue()
                        .get(keyIncrement(auctionId));
                 String leader = redisTemplate.opsForValue()    // ← add this
            .get(keyLeader(auctionId));  

                yield BidResponse.builder()
                        .status("REJECTED")
                        .reason("Bid too low. Minimum next bid: ₹" +
                                (new BigDecimal(currentBid)
                                 .add(new BigDecimal(increment)).toPlainString()))
                        .auctionId(auctionId)
                        .yourBidAmount(bidAmount)
                        .currentHighestBid(new BigDecimal(currentBid))
                        .currentLeader(leader != null && !leader.isEmpty()   // ← fix
                    ? leader : "No bids yet")    
                        .placedAt(LocalDateTime.now())
                        .build();
            }
            default -> BidResponse.builder()
                    .status("REJECTED")
                    .reason("Unknown error. Please try again.")
                    .auctionId(auctionId)
                    .yourBidAmount(bidAmount)
                    .placedAt(LocalDateTime.now())
                    .build();
        };
    }
}