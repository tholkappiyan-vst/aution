package com.example.aution.dto.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.aution.entity.AuctionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clean response object — never exposes internal entity structure directly.
 * Maps from AuctionDetailsEntity + ItemDetailsEntity into a flat response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionResponse {

    // Auction info
    private Long auctionId;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal startingPrice;
    private BigDecimal currentHighestBid;
    private BigDecimal reservePrice;
    private BigDecimal minimumIncrement;

    // Anti-sniper info
    private boolean enableAutoExtension;
    private Integer extensionWindowMinutes;
    private Integer extensionDurationMinutes;

    // Auctioneer info
    private Long auctioneerId;
    private String auctioneerUsername;
    private String companyName;

    // Item info
    private Long itemId;
    private String itemName;
    private String itemDescription;
    private String itemCategory;
    private String itemCondition;
    private BigDecimal estimatedValue;
    private String itemImageUrl;
    private List<String> secondaryImageUrls;
    private boolean certifiedAuthentic;
    private String certificateNumber;
}