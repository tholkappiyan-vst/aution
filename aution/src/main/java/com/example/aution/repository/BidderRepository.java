package com.example.aution.repository;



import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aution.entity.BidderEntity;

@Repository
public interface BidderRepository extends JpaRepository<BidderEntity, Long> {
    Optional<BidderEntity> findByUsername(String username);
}
