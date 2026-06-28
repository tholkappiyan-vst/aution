package com.example.aution.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.StaticScriptSource;

/**
 * RedisConfig wires up three things:
 *
 *  1. RedisTemplate<String, String>
 *     — typed template for all Redis operations (GET, SET, LPUSH, PUBLISH)
 *     — String serializers on both key and value (human-readable in Redis CLI)
 *
 *  2. bidLuaScript
 *     — the atomic Lua script that validates and accepts bids
 *     — loaded once at startup, cached by Redis as a SHA hash
 *     — returns a status string: "OK", "AUCTION_INACTIVE", "BID_TOO_LOW"
 *
 *  3. RedisMessageListenerContainer
 *     — listens for Redis Pub/Sub messages on bid-update channels
 *     — routes them to BidUpdateListener for WebSocket broadcast
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serializers — keys and values stored as plain UTF-8 strings
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * The Lua bid validation script.
     *
     * KEYS[1] = "auction:{auctionId}:status"       (e.g. "ACTIVE")
     * KEYS[2] = "auction:{auctionId}:highestBid"   (current highest bid amount)
     * KEYS[3] = "auction:{auctionId}:highestBidder" (username of current leader)
     * KEYS[4] = "auction:{auctionId}:minIncrement" (minimum bid jump)
     * KEYS[5] = "auction:{auctionId}:bidQueue"     (write-behind queue for PostgreSQL)
     * KEYS[6] = "auction:{auctionId}:pubsub"       (pub/sub channel for WebSocket)
     *
     * ARGV[1] = bidAmount  (the new bid being placed)
     * ARGV[2] = bidderUsername
     * ARGV[3] = timestamp  (millis from client — preserved for audit)
     *
     * Returns:
     *  "AUCTION_INACTIVE" — auction is not ACTIVE in Redis
     *  "BID_TOO_LOW"      — bid does not meet minimumIncrement requirement
     *  "OK"               — bid accepted, price updated, event published
     *
     * Why Lua?
     * Redis executes Lua scripts atomically — no other command runs between
     * any two lines of this script. This means two simultaneous bids at the
     * same amount cannot both pass the price check. The first one updates
     * the price; the second one sees the updated price and returns BID_TOO_LOW.
     * This is the ONLY correct solution to the simultaneous bid problem.
     */
    @Bean
    public DefaultRedisScript<String> bidLuaScript() {
        String lua =
            // Step 1: Check auction is ACTIVE
            "local status = redis.call('GET', KEYS[1]) " +
            "if status ~= 'ACTIVE' then return 'AUCTION_INACTIVE' end " +

            // Step 2: Get current highest bid (default 0 if no bids yet)
            "local currentBid = tonumber(redis.call('GET', KEYS[2]) or '0') " +
            "local minIncrement = tonumber(redis.call('GET', KEYS[4]) or '0') " +
            "local newBid = tonumber(ARGV[1]) " +

            // Step 3: Validate bid amount — must exceed current + minimum increment
            "if newBid <= (currentBid + minIncrement - 0.0001) then " +
            "  return 'BID_TOO_LOW' " +
            "end " +

            // Step 4: Update highest bid and bidder atomically
            "redis.call('SET', KEYS[2], ARGV[1]) " +
            "redis.call('SET', KEYS[3], ARGV[2]) " +

            // Step 5: Push to write-behind queue for async PostgreSQL persistence
            // Format: "bidAmount:bidderUsername:timestamp"
            "redis.call('LPUSH', KEYS[5], ARGV[1] .. ':' .. ARGV[2] .. ':' .. ARGV[3]) " +

            // Step 6: Publish event to Redis Pub/Sub channel
            // BidUpdateListener picks this up and broadcasts via WebSocket
            "redis.call('PUBLISH', KEYS[6], ARGV[1] .. ':' .. ARGV[2]) " +

            "return 'OK'";

        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptSource(new StaticScriptSource(lua));
        script.setResultType(String.class);
        return script;
    }

    /**
     * Container that manages Redis Pub/Sub listener threads.
     * Listeners (BidUpdateListener) are registered in BidService.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}