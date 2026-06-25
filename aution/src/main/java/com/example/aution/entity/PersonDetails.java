package com.example.aution.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "person_details",
    indexes = {
        @Index(name = "idx_person_email",    columnList = "email",    unique = true),
        @Index(name = "idx_person_username", columnList = "username", unique = true)
    }
)
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class PersonDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Identity ---
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNumber;

    // --- Auth credentials (consolidated here so CustomUserDetailsService
    //     only needs to query ONE table regardless of role) ---
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // --- Role routing flag (used by CustomUserDetailsService) ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType; // ADMIN, AUCTIONEER, BIDDER
}