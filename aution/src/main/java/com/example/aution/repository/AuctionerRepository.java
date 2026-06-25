package com.example.aution.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aution.entity.AuctionerEntity;

@Repository
public interface AuctionerRepository extends JpaRepository<AuctionerEntity, Long> {
    Optional<AuctionerEntity> findByPersonDetailsUsername(String username);
}
