package com.example.Stage.Controller;

import com.example.Stage.Model.Coverage;
import com.example.Stage.Repository.CoverageRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/coverages")
@CrossOrigin(origins = "http://localhost:4200")
public class GlobalCoverageController {

    private final CoverageRepository coverageRepository;
    public GlobalCoverageController(CoverageRepository coverageRepository) { this.coverageRepository = coverageRepository; }

    @PreAuthorize("hasRole('ROLE_app_admin')")
    @GetMapping
    public List<Coverage> getAllCoverages() { return coverageRepository.findAll(); }
}