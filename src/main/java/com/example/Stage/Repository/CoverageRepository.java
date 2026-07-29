package com.example.Stage.Repository;

import com.example.Stage.Model.Coverage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CoverageRepository extends JpaRepository<Coverage, Long> {
    List<Coverage> findByPolicyPolicyNumber(String policyNumber);
}