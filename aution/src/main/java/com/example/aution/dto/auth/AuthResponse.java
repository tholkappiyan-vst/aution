package com.example.aution.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;            // The JWT — client stores this and sends on every request
    private String username;
    private String role;             // e.g. "ROLE_ADMIN" — useful for frontend routing
    private Long userId;             // PersonDetails.id — useful for profile fetching
}