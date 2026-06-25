package com.example.aution.dto.auth;

import com.example.aution.entity.UserType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String username;
    private String password;
    private UserType userType;       // ADMIN, AUCTIONEER, or BIDDER

    // Auctioneer-specific — ignored for ADMIN and BIDDER registrations
    private String companyName;
}
