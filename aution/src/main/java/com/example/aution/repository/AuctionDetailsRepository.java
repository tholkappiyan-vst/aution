package com.example.aution.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aution.entity.AuctionDetailsEntity;
import com.example.aution.entity.AuctionStatus;

@Repository
public interface AuctionDetailsRepository extends JpaRepository<AuctionDetailsEntity, Long> {
    // Finds active auctions so the engine can track them or sync them with Redis
    List<AuctionDetailsEntity> findByStatus(AuctionStatus status);
}
