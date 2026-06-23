
package com.example.aution.repository;

import com.example.aution.entity.AuctionRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AuctionRegistrationRepository extends JpaRepository<AuctionRegistrationEntity, Long> {
    // Used to check if a bidder is already registered before letting them bid
    Optional<AuctionRegistrationEntity> findByAuctionIdAndBidderId(Long auctionId, Long bidderId);
}