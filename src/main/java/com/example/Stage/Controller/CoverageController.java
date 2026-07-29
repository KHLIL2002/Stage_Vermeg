package com.example.Stage.Controller;

import com.example.Stage.Model.Coverage;
import com.example.Stage.Repository.CoverageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies/{policyNumber}/coverages")
@CrossOrigin(origins = "http://localhost:4200")
public class CoverageController {

    private final CoverageRepository coverageRepository;

    public CoverageController(CoverageRepository coverageRepository) {
        this.coverageRepository = coverageRepository;
    }

    @GetMapping
    public List<Coverage> getCoverages(@PathVariable String policyNumber) {
        return coverageRepository.findByPolicyPolicyNumber(policyNumber);
    }
}