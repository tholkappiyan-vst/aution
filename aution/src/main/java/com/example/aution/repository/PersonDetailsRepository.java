package com.example.aution.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aution.entity.PersonDetails;
import com.example.aution.entity.UserType;

@Repository
public interface PersonDetailsRepository extends JpaRepository<PersonDetails, Long> {

    // Used by CustomUserDetailsService on every login attempt
    Optional<PersonDetails> findByUsername(String username);

    // Useful for registration duplicate checks
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Used by AuthService to find role-specific entity after registration
    Optional<PersonDetails> findByUsernameAndUserType(String username, UserType userType);
}
