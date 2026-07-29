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
@AllArgsConstructor
@NoArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String identifier;

    private LocalDate effectiveDate;

    private LocalDate issuingDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private String status; // PAID, PENDING, UNPAID

    private BigDecimal billAmount;

    private BigDecimal paidAmount;

    private String currency;

    private String reminderStatus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id")
    @JsonIgnoreProperties({"roles", "coverages", "bills", "endorsements"})
    private Policy policy;

    /** Numéro de police exposé à plat pour le front (bill.policyNumber). */
    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("policyNumber")
    public String getPolicyNumber() {
        return policy != null ? policy.getPolicyNumber() : null;
    }
}