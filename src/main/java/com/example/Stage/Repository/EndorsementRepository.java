package com.example.Stage.Repository;

import com.example.Stage.Model.Endorsement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EndorsementRepository extends JpaRepository<Endorsement, Long> {
    List<Endorsement> findByPolicyPolicyNumber(String policyNumber);
    List<Endorsement> findByStatus(String status);
}