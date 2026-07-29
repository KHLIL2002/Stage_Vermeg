package com.example.Stage.Controller;

import com.example.Stage.Model.Endorsement;
import com.example.Stage.Repository.EndorsementRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies/{policyNumber}/endorsements")
@CrossOrigin(origins = "http://localhost:4200")
public class EndorsementController {

    private final EndorsementRepository endorsementRepository;

    public EndorsementController(EndorsementRepository endorsementRepository) {
        this.endorsementRepository = endorsementRepository;
    }

    @GetMapping
    public List<Endorsement> getEndorsements(@PathVariable String policyNumber, @RequestParam(required = false) String status) {
        List<Endorsement> endorsements = endorsementRepository.findByPolicyPolicyNumber(policyNumber);
        if (status != null) {
            return endorsements.stream().filter(e -> status.equals(e.getStatus())).toList();
        }
        return endorsements;
    }
}