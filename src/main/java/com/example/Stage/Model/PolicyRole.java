package com.example.Stage.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class PolicyRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id")
    @JsonIgnoreProperties({"roles", "coverages", "bills", "endorsements"})
    private Policy policy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "third_party_id")
    private ThirdParty thirdParty;

    @Column(nullable = false)
    private String roleType; // HOLDER, BENEFICIARY, PAYER, LIFE_ASSURED, BROKER
}