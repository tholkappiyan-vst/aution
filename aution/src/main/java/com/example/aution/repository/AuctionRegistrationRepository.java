package com.example.aution.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aution.entity.AuctionDetailsEntity;
import com.example.aution.entity.AuctionRegistrationEntity;
import com.example.aution.entity.BidderEntity;

@Repository
public interface AuctionRegistrationRepository
        extends JpaRepository<AuctionRegistrationEntity, Long> {

    // Check if bidder already registered for this auction (duplicate guard)
    boolean existsByAuctionAndBidder(
            AuctionDetailsEntity auction, BidderEntity bidder);

    // Find specific registration for unregister operation
    Optional<AuctionRegistrationEntity> findByAuctionAndBidder(
            AuctionDetailsEntity auction, BidderEntity bidder);

    // All auctions a bidder has registered for
    List<AuctionRegistrationEntity> findByBidder(BidderEntity bidder);
}