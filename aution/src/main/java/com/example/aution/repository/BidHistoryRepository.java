package com.example.aution.repository;

import com.example.aution.entity.AuctionDetailsEntity;
import com.example.aution.entity.BidHistoryEntity;
import com.example.aution.entity.BidderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidHistoryRepository extends JpaRepository<BidHistoryEntity, Long> {

    // All bids for a specific auction (leaderboard / winner resolution)
    List<BidHistoryEntity> findByAuctionOrderByBidAmountDesc(AuctionDetailsEntity auction);

    // All bids by a specific bidder across all auctions
    List<BidHistoryEntity> findByBidderOrderByPlacedAtDesc(BidderEntity bidder);
}