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
public class Coverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String identifier;

    private String label;

    private String status; // ACTIVE, CANCELLED

    private String coverageType;

    private BigDecimal capital;

    private String capitalCurrency;

    private BigDecimal premium;

    private String premiumCurrency;

    private LocalDate beginDate;

    private LocalDate endDate;

    private boolean mainCover;

    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id")
    @JsonIgnoreProperties({"roles", "coverages", "bills", "endorsements"})
    private Policy policy;
}