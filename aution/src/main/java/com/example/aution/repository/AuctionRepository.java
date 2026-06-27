package com.example.aution.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.aution.entity.AuctionDetailsEntity;
import com.example.aution.entity.AuctionStatus;
import com.example.aution.entity.AuctionerEntity;

@Repository
public interface AuctionRepository extends JpaRepository<AuctionDetailsEntity, Long> {

    // All auctions owned by a specific auctioneer
    List<AuctionDetailsEntity> findByAuctioner(AuctionerEntity auctioner);

    // Ownership check — used before any update/delete
    Optional<AuctionDetailsEntity> findByIdAndAuctioner(Long id, AuctionerEntity auctioner);

    // Scheduler query: find SCHEDULED auctions whose startTime has passed
    List<AuctionDetailsEntity> findByStatusAndStartTimeBefore(
            AuctionStatus status, LocalDateTime now);

    // Scheduler query: find ACTIVE auctions whose endTime has passed
    List<AuctionDetailsEntity> findByStatusAndEndTimeBefore(
            AuctionStatus status, LocalDateTime now);

    // Public listing — only show ACTIVE and SCHEDULED auctions
    @Query("SELECT a FROM AuctionDetailsEntity a WHERE a.status IN :statuses")
    List<AuctionDetailsEntity> findByStatusIn(@Param("statuses") List<AuctionStatus> statuses);
}