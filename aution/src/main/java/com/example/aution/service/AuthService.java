package com.example.aution.service;

import com.example.aution.dto.auth.AuthResponse;
import com.example.aution.dto.auth.LoginRequest;
import com.example.aution.dto.auth.RegisterRequest;
import com.example.aution.entity.*;
import com.example.aution.repository.AdminRepository;
import com.example.aution.repository.AuctionerRepository;
import com.example.aution.repository.BidderRepository;
import com.example.aution.repository.PersonDetailsRepository;
import com.example.aution.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService handles two operations:
 *
 *  register() — validates uniqueness, BCrypts password, persists PersonDetails
 *               + role-specific entity, generates and returns a JWT.
 *
 *  login()    — delegates credential verification to AuthenticationManager
 *               (which uses CustomUserDetailsService + BCrypt internally),
 *               then generates and returns a JWT.
 *
 * Notice: login() never manually checks the password — that is AuthenticationManager's job.
 * Delegating this is the correct Spring Security pattern.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonDetailsRepository personDetailsRepository;
    private final AdminRepository adminRepository;
    private final AuctionerRepository auctionerRepository;
    private final BidderRepository bidderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ── Registration ─────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Guard: prevent duplicate username or email across all roles
        if (personDetailsRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (personDetailsRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Build and save PersonDetails with hashed password
        PersonDetails person = PersonDetails.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .userType(request.getUserType())
                .build();

        PersonDetails savedPerson = personDetailsRepository.save(person);

        // Create the role-specific entity and link it to PersonDetails
        createRoleEntity(request, savedPerson);

        // Generate JWT immediately after registration — user is logged in
        String role = "ROLE_" + savedPerson.getUserType().name();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedPerson.getUsername())
                .password(savedPerson.getPassword())
                .authorities(role)
                .build();

        String token = jwtService.generateToken(userDetails, savedPerson.getId(), role);

        return AuthResponse.builder()
                .token(token)
                .username(savedPerson.getUsername())
                .role(role)
                .userId(savedPerson.getId())
                .build();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {

        // AuthenticationManager internally calls CustomUserDetailsService.loadUserByUsername()
        // then compares the provided password against the BCrypt hash in DB.
        // Throws BadCredentialsException automatically if credentials are wrong.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // At this point credentials are verified — safe to load and build token
        PersonDetails person = personDetailsRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("User disappeared between auth and lookup"));

        String role = "ROLE_" + person.getUserType().name();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(person.getUsername())
                .password(person.getPassword())
                .authorities(role)
                .build();

        String token = jwtService.generateToken(userDetails, person.getId(), role);

        return AuthResponse.builder()
                .token(token)
                .username(person.getUsername())
                .role(role)
                .userId(person.getId())
                .build();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Creates the role-specific entity (Admin/Auctioner/Bidder) and links
     * it to the already-persisted PersonDetails.
     * Uses a switch on UserType — clean, exhaustive, no if/else chain.
     */
    private void createRoleEntity(RegisterRequest request, PersonDetails savedPerson) {
        switch (request.getUserType()) {
            case ADMIN -> adminRepository.save(
                    AdminEntity.builder()
                            .personDetails(savedPerson)
                            .build()
            );
            case AUCTIONEER -> auctionerRepository.save(
                    AuctionerEntity.builder()
                            .companyName(request.getCompanyName())
                            .personDetails(savedPerson)
                            .build()
            );
            case BIDDER -> bidderRepository.save(
                    BidderEntity.builder()
                            .personDetails(savedPerson)
                            .build()
            );
            default -> throw new IllegalArgumentException(
                    "Unknown UserType: " + request.getUserType()
            );
        }
    }
}
