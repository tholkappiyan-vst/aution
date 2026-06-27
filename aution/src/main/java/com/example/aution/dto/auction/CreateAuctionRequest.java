package com.example.aution.dto.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single request DTO that creates both the ItemDetailsEntity
 * and AuctionDetailsEntity in one call.
 *
 * The auctioneer provides item details + auction scheduling in one payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuctionRequest {

    // ── Item Details ──────────────────────────────────────────────────────────

    @NotBlank(message = "Item name is required")
    private String itemName;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String itemDescription;

    @NotBlank(message = "Item category is required")
    private String itemCategory;         // e.g. "Electronics", "Fine Art"

    @NotBlank(message = "Item condition is required")
    private String itemCondition;        // "NEW", "MINT", "USED_GOOD", "REFURBISHED"

    @NotNull(message = "Estimated value is required")
    @DecimalMin(value = "0.01", message = "Estimated value must be greater than 0")
    private BigDecimal estimatedValue;

    private String itemImageUrl;         // Primary image URL
    private List<String> secondaryImageUrls;

    private String upc;                  // Universal Product Code (optional)
    private Double weightKg;
    private Double widthCm;
    private Double heightCm;
    private Double depthCm;

    private boolean certifiedAuthentic;
    private String certificateNumber;
    private String provenance;           // Ownership history for high-value items

    // ── Auction Scheduling ────────────────────────────────────────────────────

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    // ── Auction Financial Settings ────────────────────────────────────────────

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    private BigDecimal startingPrice;

    @DecimalMin(value = "0.01", message = "Reserve price must be greater than 0")
    private BigDecimal reservePrice;     // Optional minimum to sell

    @NotNull(message = "Minimum increment is required")
    @DecimalMin(value = "0.01", message = "Minimum increment must be greater than 0")
    private BigDecimal minimumIncrement;

    // ── Anti-Sniper Settings (optional — defaults applied in service) ─────────

    private Boolean enableAutoExtension;      // default: true
    private Integer extensionWindowMinutes;   // default: 2
    private Integer extensionDurationMinutes; // default: 5
}