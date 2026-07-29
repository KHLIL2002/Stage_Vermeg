package com.example.Stage.Controller;

import com.example.Stage.Model.Endorsement;
import com.example.Stage.Repository.EndorsementRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/endorsements")
@CrossOrigin(origins = "http://localhost:4200")
public class GlobalEndorsementController {

    private final EndorsementRepository endorsementRepository;
    public GlobalEndorsementController(EndorsementRepository endorsementRepository) { this.endorsementRepository = endorsementRepository; }

    @GetMapping
    public List<Endorsement> getAllEndorsements() { return endorsementRepository.findAll(); }
}