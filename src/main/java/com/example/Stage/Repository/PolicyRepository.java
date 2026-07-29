package com.example.Stage.Repository;

import com.example.Stage.Model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    // ── Ownership check ──────────────────────────────────────────────────────
    @Query("SELECT p FROM Policy p WHERE p.policyNumber = :num AND p.holder.identifier = :userId")
    Optional<Policy> findByPolicyNumberAndOwner(@Param("num") String num, @Param("userId") String userId);

    // ── Basic lookups ────────────────────────────────────────────────────────
    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByHolderIdentifier(String identifier);

    // ── Filter by status ─────────────────────────────────────────────────────
    List<Policy> findByStatus(String status);

    /** Status filter scoped to a specific holder (for non-admin users). */
    List<Policy> findByStatusAndHolderIdentifier(String status, String holderIdentifier);

    // ── Filter by holder name ─────────────────────────────────────────────────
    List<Policy> findByHolderNameContainingIgnoreCase(String name);

    /** Holder-name filter scoped to a specific holder (for non-admin users). */
    List<Policy> findByHolderNameContainingIgnoreCaseAndHolderIdentifier(String name, String holderIdentifier);

    // ── Filter by product code ────────────────────────────────────────────────
    List<Policy> findByProductProductCode(String productCode);

    /** Product-code filter scoped to a specific holder (for non-admin users). */
    List<Policy> findByProductProductCodeAndHolderIdentifier(String productCode, String holderIdentifier);

    // ── Search by policy number ───────────────────────────────────────────────
    List<Policy> findByPolicyNumberContainingIgnoreCase(String policyNumber);

    List<Policy> findByPolicyNumberStartingWith(String prefix);

    // ── Legacy / misc ─────────────────────────────────────────────────────────
    List<Policy> findByHolderId(Long holderId);
}