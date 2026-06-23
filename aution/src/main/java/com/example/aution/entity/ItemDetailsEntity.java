package com.example.aution.entity;


import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_details")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ItemDetailsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Core Identity ---
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String category; // e.g., "Electronics", "Fine Art", "Collectibles"

    // --- Product Identifiers (Industry Standard) ---
    
    private String upc;      // Universal Product Code if dealing with retail goods

    // --- Valuation & Attributes ---
    @Column(nullable = false)
    private Double estimatedValue;

    @Column(nullable = false)
    private String condition; // e.g., "NEW", "MINT", "USED_GOOD", "REFURBISHED"

    // --- Media Assets ---
    private String imageUrl; // Primary display image
    
    @ElementCollection // Stores multiple images without needing a full separate entity
    @CollectionTable(name = "item_images", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "image_url")
    List<String> secondaryImageUrls;

    // --- Physical Shipping Specs (Crucial for high-value logistics) ---
    private Double weightKg;
    private Double widthCm;
    private Double heightCm;
    private Double depthCm;

    // --- Authenticity & Security ---
    private boolean isCertifiedAuthentic;
    private String certificateNumber; // Verification ID from a 3rd party grading agency
    
    @Lob // For long metadata or detailed historical provenance notes
    private String provenance; // The history of ownership for high-value collectibles/art

    // --- Audit & Tracking ---
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}