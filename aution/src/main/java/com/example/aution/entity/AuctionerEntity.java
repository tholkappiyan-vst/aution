package com.example.aution.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auctioners")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String companyName;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    private PersonDetails personDetails;
}
