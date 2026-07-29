package com.example.Stage.Repository;

import com.example.Stage.Model.ThirdParty;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ThirdPartyRepository extends JpaRepository<ThirdParty, Long> {
    Optional<ThirdParty> findByIdentifier(String identifier);
    List<ThirdParty> findByNameContainingIgnoreCase(String name);
}