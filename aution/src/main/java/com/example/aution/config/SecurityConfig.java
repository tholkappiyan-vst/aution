package com.example.aution.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.aution.security.CustomUserDetailsService;
import com.example.aution.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;


/**
 * SecurityConfig is the central wiring point for the entire security layer.
 *
 * Key decisions made here:
 *  - STATELESS session — no cookies, no HttpSession. Every request must carry a JWT.
 *  - CSRF disabled — not needed for stateless REST APIs (CSRF attacks require session cookies).
 *  - Role-based route rules — defined once here, enforced automatically on every request.
 *  - JwtAuthFilter runs before Spring's UsernamePasswordAuthenticationFilter.
 *
 * @EnableMethodSecurity also enabled — lets you use @PreAuthorize("hasRole('ADMIN')")
 * directly on controller methods for fine-grained access control.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // Enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless REST APIs don't use session cookies
            .csrf(AbstractHttpConfigurer::disable)
             .headers(headers -> headers
        .frameOptions(frame -> frame.disable())
        )
            // Define route-level access rules
            .authorizeHttpRequests(auth -> auth

                  // OpenAPI / Swagger UI (useful during development)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui/index.html",
                    "/error"
                    
                ).permitAll()

                // ── Public endpoints (no JWT required) ──────────────────────
                .requestMatchers("/auth/register", "/auth/login").permitAll()
                .requestMatchers("/redis/ping").permitAll() 
                .requestMatchers("/ws/**", "/ws/info/**").permitAll()
                // Public read-only auction/item browsing (your requirement)
                .requestMatchers(HttpMethod.GET, "/auctions/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/items/**").permitAll()

                

                // ── Admin-only endpoints ─────────────────────────────────────
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // ── Auctioneer endpoints ─────────────────────────────────────
                .requestMatchers("/auctioneer/**").hasRole("AUCTIONEER")

                // ── Bidder endpoints ─────────────────────────────────────────
                .requestMatchers("/bidder/**").hasRole("BIDDER")

                // ── Shared protected endpoints ───────────────────────────────
                // e.g. bidding actions require either BIDDER or ADMIN
                .requestMatchers("/bids/**").hasAnyRole("BIDDER", "ADMIN")

                // Everything else requires authentication (any valid role)
                .anyRequest().authenticated()
            )

            // Stateless — Spring will never create or use an HttpSession
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Wire our custom authentication provider (BCrypt + UserDetailsService)
            .authenticationProvider(authenticationProvider())

            // Insert JwtAuthFilter BEFORE Spring's default username/password filter
            // This ensures every request is checked for a valid JWT first
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider connects Spring Security's AuthenticationManager
     * to our CustomUserDetailsService and BCryptPasswordEncoder.
     *
     * When AuthenticationManager.authenticate() is called during login,
     * it uses this provider to: load user → verify BCrypt hash → return auth token.
     */
    
    @Bean
       public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
    }
    /**
     * BCryptPasswordEncoder with strength 12 (2^12 = 4096 hashing rounds).
     * Strength 10 is the default; 12 is the FAANG-level recommendation —
     * slow enough to resist brute-force, fast enough for real traffic.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Exposes the AuthenticationManager bean so AuthService can call
     * authManager.authenticate() during login without circular dependencies.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
