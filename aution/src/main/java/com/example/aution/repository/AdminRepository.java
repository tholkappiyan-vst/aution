package com.example.aution.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aution.entity.AdminEntity;


// ── AdminRepository ───────────────────────────────────────────────────────────
@Repository
public interface AdminRepository extends JpaRepository<AdminEntity, Long> {
    // Route through PersonDetails — no username field on AdminEntity itself
    Optional<AdminEntity> findByPersonDetailsUsername(String username);
}


