package com.example.Stage.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroContrat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Produit produit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut;

    @Column(nullable = false)
    private LocalDate dateEffet;

    @Column(nullable = false)
    private LocalDate dateEcheance;

    @Column(nullable = false)
    private BigDecimal primeAnnuelle;

    @Column(nullable = false)
    private BigDecimal plafondGarantie;

    @Column(nullable = false)
    private BigDecimal franchise;

    @Column(length = 2000)
    private String clausesParticulieres;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL)
    private List<Tiers> tiers;

    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum Produit {
        AUTO_TIERS, AUTO_TOUS_RISQUES,
        HABITATION_LOCATAIRE, HABITATION_PROPRIETAIRE,
        SANTE_INDIVIDUELLE, SANTE_COLLECTIVE,
        VIE, MULTIRISQUE_PRO, RC_PRO, PREVOYANCE
    }

    public enum Statut {
        EN_VIGUEUR, RESILIE, SUSPENDU, EN_ATTENTE
    }
}