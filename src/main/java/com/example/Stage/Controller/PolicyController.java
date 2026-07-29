package com.example.Stage.Controller;

import com.example.Stage.Model.Policy;
import com.example.Stage.Repository.PolicyRepository;
import com.example.Stage.Service.ToolRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@CrossOrigin(origins = "http://localhost:4200")
public class PolicyController {

    private final PolicyRepository policyRepository;
    private final ToolRegistry toolRegistry;

    public PolicyController(PolicyRepository policyRepository, ToolRegistry toolRegistry) {
        this.policyRepository = policyRepository;
        this.toolRegistry = toolRegistry;

    }

    @PreAuthorize("hasRole('ROLE_app_admin')")
    @GetMapping
    public List<Policy> getPolicies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String holderName,
            @RequestParam(required = false) String productCode) {

        if (status != null) return policyRepository.findByStatus(status);
        if (holderName != null) return policyRepository.findByHolderNameContainingIgnoreCase(holderName);
        if (productCode != null) return policyRepository.findByProductProductCode(productCode);
        return policyRepository.findAll();
    }

    @GetMapping("/{policyNumber}")
    public Policy getPolicyDetail(@PathVariable String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new RuntimeException("Police introuvable : " + policyNumber));
    }

    @GetMapping("/{policyNumber}/summary")
    public Policy getPolicySummary(@PathVariable String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new RuntimeException("Police introuvable : " + policyNumber));
    }

    @PreAuthorize("hasRole('ROLE_app_admin')")
    @PostMapping
    public ResponseEntity<?> createManual(@RequestBody Map<String, Object> requestData) {
        // On appelle la fonction de création que nous avons déjà codée dans le ToolRegistry
        // Cela évite de réécrire toute la logique de génération du numéro 001-XXX
        String result = toolRegistry.executeTool("createSubscription", requestData);

        if (result.contains("SUCCESS")) {
            return ResponseEntity.ok(Map.of("message", result));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
    }
}