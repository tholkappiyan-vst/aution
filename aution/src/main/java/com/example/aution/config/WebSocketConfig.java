package com.example.aution.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig sets up STOMP (Simple Text Oriented Messaging Protocol)
 * over WebSocket.
 *
 * How it works:
 *  - Client connects to /ws endpoint (HTTP upgrade → WebSocket)
 *  - Client subscribes to /topic/auction/{id} to receive live bid updates
 *  - Client sends bids to /app/bid/{id} (handled by @MessageMapping in BidController)
 *  - Server broadcasts to /topic/auction/{id} after every accepted bid
 *
 * STOMP is used instead of raw WebSocket because it gives us:
 *  - Topic-based pub/sub (one broadcast reaches all subscribers)
 *  - Built-in destination routing (/app vs /topic)
 *  - SockJS fallback for clients that don't support WebSocket
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic prefix — server-to-client broadcasts (price updates to all bidders)
        // /queue prefix — server-to-specific-client messages (personal notifications)
        registry.enableSimpleBroker("/topic", "/queue");

        // /app prefix — client-to-server messages (placing a bid)
        // Routes to @MessageMapping methods in controllers
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // restrict to your frontend URL in production
                .withSockJS();  // SockJS fallback for browsers that don't support WebSocket
    }
}