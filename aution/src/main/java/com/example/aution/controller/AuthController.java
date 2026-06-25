package com.example.aution.controller;

import com.example.aution.dto.auth.AuthResponse;
import com.example.aution.dto.auth.LoginRequest;
import com.example.aution.dto.auth.RegisterRequest;
import com.example.aution.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController exposes two public endpoints:
 *
 *  POST /auth/register  — creates a new user account (any role)
 *  POST /auth/login     — returns a JWT for valid credentials
 *
 * Both are marked permitAll() in SecurityConfig — no JWT required to reach them.
 * Every other endpoint in the platform requires a valid JWT.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     *
     * Request body example:
     * {
     *   "firstName": "Ravi",
     *   "lastName":  "Kumar",
     *   "email":     "ravi@example.com",
     *   "username":  "ravi123",
     *   "password":  "Secret@123",
     *   "userType":  "BIDDER",
     *   "companyName": null          // only needed for AUCTIONEER
     * }
     *
     * Response: JWT token + role + userId (201 Created)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with existing credentials.
     *
     * Request body example:
     * {
     *   "username": "ravi123",
     *   "password": "Secret@123"
     * }
     *
     * Response: JWT token + role + userId (200 OK)
     * Throws 403 automatically if credentials are wrong (Spring Security handles it).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
