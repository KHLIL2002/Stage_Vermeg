package com.example.Stage.Repository;

import com.example.Stage.Model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByPolicyPolicyNumber(String policyNumber);
    List<Bill> findByStatus(String status);
}