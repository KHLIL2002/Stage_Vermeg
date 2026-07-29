package com.example.Stage.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class ThirdParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String type; // PHYSICAL_PERSON or LEGAL_ENTITY

    @Column(nullable = false)
    private String name;

    private String nationalIdentifier;

    private String nationalIdentifierCountry;

    // Physical person fields
    private String firstName;
    private String secondName;
    private LocalDate birthDate;
    private String gender;

    // Legal entity fields
    private String legalForm;
    private String tradeRegister;

    // Contact
    private String email;
    private String phone;

    // Address
    private String street;
    private String postalCode;
    private String town;
    private String country;
}