package com.example.aution.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter runs once per HTTP request (OncePerRequestFilter guarantee).
 *
 * Execution order on every protected request:
 *  1. Extract "Authorization: Bearer <token>" header
 *  2. Parse and validate the JWT using JwtService
 *  3. Load UserDetails from DB (only if SecurityContext is empty)
 *  4. Set authentication into SecurityContext
 *  5. Chain continues to the actual controller
 *
 * If any step fails (missing header, expired token, wrong signature),
 * we simply don't set the SecurityContext — Spring Security will
 * return 403 automatically.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Extract the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // If header is missing or doesn't start with "Bearer ", skip this filter
        // Public endpoints (/auth/register, /auth/login) will pass through here
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Strip "Bearer " prefix to get the raw token
        final String jwt = authHeader.substring(7);

        // Step 3: Extract username from the token
        // If the token is malformed, JwtService throws — filter passes
        // through and Spring Security handles the 403
        final String username;
        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 4: Only process if username exists and no auth is already set
        // (Prevents re-processing on the same request if another filter ran first)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Step 5: Load full UserDetails from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Step 6: Validate token against the loaded UserDetails
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Step 7: Build Spring Security authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                        // credentials null — already verified
                                userDetails.getAuthorities() // roles from UserDetails
                        );

                // Attach request metadata (IP, session) to the auth token
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 8: Register authentication in the SecurityContext
                // From this point, Spring Security treats this request as authenticated
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Step 9: Continue the filter chain regardless
        filterChain.doFilter(request, response);
    }
}
