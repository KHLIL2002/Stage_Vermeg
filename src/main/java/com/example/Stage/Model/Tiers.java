package com.example.Stage.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Tiers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroTiers;

    @Column(nullable = false)
    private String nom;

    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleTiers role;

    private String email;

    private String telephone;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contrat_id")
    private Contrat contrat;

    public enum RoleTiers {
        BENEFICIAIRE, ASSURE_ADDITIONNEL, COURTIER,
        CONDUCTEUR_SECONDAIRE, AYANT_DROIT, MANDATAIRE
    }
}