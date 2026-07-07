package com.example.Stage.Repository;

import com.example.Stage.Model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findBySegment(Client.Segment segment);

    List<Client> findByNomContainingIgnoreCase(String nom);

    List<Client> findByVilleContainingIgnoreCase(String ville);
}