package com.example.Stage.Controller;

import com.example.Stage.Model.PolicyRole;
import com.example.Stage.Repository.PolicyRoleRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies/{policyNumber}/roles")
@CrossOrigin(origins = "http://localhost:4200")
public class PolicyRoleController {

    private final PolicyRoleRepository policyRoleRepository;

    public PolicyRoleController(PolicyRoleRepository policyRoleRepository) {
        this.policyRoleRepository = policyRoleRepository;
    }

    @GetMapping
    public List<PolicyRole> getRoles(@PathVariable String policyNumber) {
        return policyRoleRepository.findByPolicyPolicyNumber(policyNumber);
    }
}