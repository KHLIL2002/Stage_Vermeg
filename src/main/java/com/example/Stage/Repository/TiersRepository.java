package com.example.Stage.Repository;

import com.example.Stage.Model.Tiers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TiersRepository extends JpaRepository<Tiers, Long> {

    List<Tiers> findByContratId(Long contratId);

    List<Tiers> findByNomContainingIgnoreCase(String nom);
}