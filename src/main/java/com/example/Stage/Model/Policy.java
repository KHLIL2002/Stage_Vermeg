package com.example.Stage.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String policyNumber;

    @Column(nullable = false)
    private String status; // ACTIVE, CANCELLED, SUSPENDED, PENDING

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "holder_id")
    private ThirdParty holder;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    private LocalDate termDate;

    private String currency;

    private String paymentPeriodicity; // MONTHLY, QUARTERLY, YEARLY

    private BigDecimal annualPremium;

    private BigDecimal surrenderValue;

    private String taxCountry;

    private String premiumType; // REGULAR, SINGLE

    private String paymentMode; // DIRECT_DEBIT, TRANSFER

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("policy")
    private List<PolicyRole> roles;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("policy")
    private List<Coverage> coverages;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("policy")
    private List<Bill> bills;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("policy")
    private List<Endorsement> endorsements;
}