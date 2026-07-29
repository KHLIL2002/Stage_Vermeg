package com.example.Stage.Repository;

import com.example.Stage.Model.PolicyRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PolicyRoleRepository extends JpaRepository<PolicyRole, Long> {
    List<PolicyRole> findByPolicyId(Long policyId);
    List<PolicyRole> findByPolicyPolicyNumber(String policyNumber);
}