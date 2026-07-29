package com.example.Stage.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Endorsement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String identifier;

    private LocalDate effectiveDate;

    @Column(nullable = false)
    private String status; // PENDING, VALIDATED, REJECTED

    private String endorsementType; // ADDITION, WITHDRAWAL, MODIFICATION

    private String endorsementSubType;

    private BigDecimal grossAmount;

    private String grossAmountCurrency;

    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id")
    @JsonIgnoreProperties({"roles", "coverages", "bills", "endorsements"})
    private Policy policy;

    /** Numéro de police exposé à plat pour le front (endorsement.policyNumber). */
    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("policyNumber")
    public String getPolicyNumber() {
        return policy != null ? policy.getPolicyNumber() : null;
    }
}