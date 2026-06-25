package com.example.aution.security;

import com.example.aution.entity.PersonDetails;
import com.example.aution.repository.PersonDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CustomUserDetailsService is the bridge between Spring Security
 * and your PersonDetails table.
 *
 * Key design decision: ALL three roles (ADMIN, AUCTIONEER, BIDDER)
 * are loaded from ONE table — PersonDetails. No if/else branching
 * across three repositories. This is what makes the JWT filter
 * clean and fast.
 *
 * Spring Security calls loadUserByUsername() automatically during
 * authentication (UsernamePasswordAuthenticationToken flow).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final PersonDetailsRepository personDetailsRepository;

    /**
     * Loads the user by username and maps their UserType to a
     * Spring Security GrantedAuthority (e.g. ROLE_ADMIN).
     *
     * The returned UserDetails object is used by:
     *  - AuthenticationManager to verify the password
     *  - JwtAuthFilter to populate the SecurityContext
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        PersonDetails person = personDetailsRepository.findByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("No user found with username: " + username)
                );

        // Map UserType enum → Spring Security role string
        // Convention: Spring Security roles must be prefixed with "ROLE_"
        String role = "ROLE_" + person.getUserType().name(); // e.g. "ROLE_ADMIN"

        return User.builder()
                .username(person.getUsername())
                .password(person.getPassword())  // Already BCrypt-hashed in DB
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
    }
}
