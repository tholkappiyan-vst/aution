package com.example.aution.listener;

import com.example.aution.dto.bid.BidUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BidUpdateListener is a Redis Pub/Sub subscriber.
 *
 * Flow:
 *  1. Redis Lua script publishes "bidAmount:bidderUsername" to channel
 *     "auction:{auctionId}:pubsub" after every accepted bid
 *  2. This listener receives the message on that channel
 *  3. It builds a BidUpdateMessage and broadcasts via WebSocket STOMP
 *     to /topic/auction/{auctionId}
 *  4. Every connected bidder receives the update instantly
 *
 * One instance of this listener is registered PER AUCTION when the
 * auction transitions to ACTIVE (done in AuctionSchedulerService).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidUpdateListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    // auctionId is set when this listener is registered for a specific auction
    private Long auctionId;

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    /**
     * Called by RedisMessageListenerContainer when a message arrives
     * on the subscribed channel.
     *
     * Message body format: "bidAmount:bidderUsername"
     * e.g. "5500.00:ravi123"
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            String[] parts = body.split(":", 2);

            if (parts.length != 2) {
                log.warn("Malformed bid update message: {}", body);
                return;
            }

            BigDecimal bidAmount = new BigDecimal(parts[0]);
            String bidderUsername = parts[1];

            BidUpdateMessage update = BidUpdateMessage.builder()
                    .auctionId(auctionId)
                    .currentHighestBid(bidAmount)
                    .currentLeader(bidderUsername)
                    .bidPlacedAt(LocalDateTime.now())
                    .message(bidderUsername + " placed ₹" + bidAmount)
                    .build();

            // Broadcast to ALL bidders subscribed to this auction's topic
            messagingTemplate.convertAndSend(
                    "/topic/auction/" + auctionId, update);

            log.debug("Broadcast bid update for auction [{}]: ₹{} by {}",
                    auctionId, bidAmount, bidderUsername);

        } catch (Exception e) {
            log.error("Error processing bid update message for auction [{}]: {}",
                    auctionId, e.getMessage());
        }
    }
}