package com.example.Stage.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroClient;

    @Column(nullable = false)
    private String nom;

    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeClient typeClient;

    @Column(nullable = false)
    private String email;

    private String telephone;

    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private String ville;

    @Column(nullable = false)
    private String codePostal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Segment segment;

    private LocalDateTime dateCreation = LocalDateTime.now();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Contrat> contrats;

    public enum TypeClient { PARTICULIER, ENTREPRISE }
    public enum Segment { STANDARD, PREMIUM, VIP }
}