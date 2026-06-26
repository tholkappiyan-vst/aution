package com.example.aution.service;

import com.example.aution.dto.auth.AuthResponse;
import com.example.aution.dto.auth.LoginRequest;
import com.example.aution.dto.auth.RegisterRequest;
import com.example.aution.entity.*;
import com.example.aution.exception.DuplicateResourceException;
import com.example.aution.exception.InvalidCredentialsException;
import com.example.aution.repository.*;
import com.example.aution.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ── Registration ──────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Throw DuplicateResourceException (→ 409) instead of IllegalArgumentException (→ 500)
        if (personDetailsRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                "Username already taken: " + request.getUsername()
            );
        }
        if (personDetailsRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                "Email is already registered: " + request.getEmail()
            );
        }

        // Build and persist PersonDetails with BCrypt-hashed password
        PersonDetails person = PersonDetails.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(request.getUserType())
                .build();

        PersonDetails savedPerson = personDetailsRepository.save(person);

        // Persist role-specific entity
        createRoleEntity(request, savedPerson);

        // Issue JWT immediately on registration
        String role = "ROLE_" + savedPerson.getUserType().name();
        UserDetails userDetails = buildUserDetails(savedPerson, role);
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
        try {
            // AuthenticationManager verifies credentials via CustomUserDetailsService
            // Throws BadCredentialsException automatically if wrong
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            // Wrap into our InvalidCredentialsException (→ 401 via GlobalExceptionHandler)
            throw new InvalidCredentialsException();
        }

        PersonDetails person = personDetailsRepository
                .findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        String role = "ROLE_" + person.getUserType().name();
        UserDetails userDetails = buildUserDetails(person, role);
        String token = jwtService.generateToken(userDetails, person.getId(), role);

        return AuthResponse.builder()
                .token(token)
                .username(person.getUsername())
                .role(role)
                .userId(person.getId())
                .build();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void createRoleEntity(RegisterRequest request, PersonDetails savedPerson) {
        switch (request.getUserType()) {
            case ADMIN -> adminRepository.save(
                    AdminEntity.builder().personDetails(savedPerson).build()
            );
            case AUCTIONEER -> auctionerRepository.save(
                    AuctionerEntity.builder()
                            .companyName(request.getCompanyName())
                            .personDetails(savedPerson)
                            .build()
            );
            case BIDDER -> bidderRepository.save(
                    BidderEntity.builder().personDetails(savedPerson).build()
            );
            default -> throw new IllegalArgumentException(
                    "Unknown UserType: " + request.getUserType()
            );
        }
    }

    private UserDetails buildUserDetails(PersonDetails person, String role) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(person.getUsername())
                .password(person.getPassword())
                .authorities(role)
                .build();
    }
}