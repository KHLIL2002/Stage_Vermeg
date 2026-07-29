package com.example.Stage.Controller;

import com.example.Stage.Model.ThirdParty;
import com.example.Stage.Repository.ThirdPartyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/third-parties")
@CrossOrigin(origins = "http://localhost:4200")
public class ThirdPartyController {

    private final ThirdPartyRepository thirdPartyRepository;

    public ThirdPartyController(ThirdPartyRepository thirdPartyRepository) {
        this.thirdPartyRepository = thirdPartyRepository;
    }

    /**
     * GET /api/third-parties
     * Admin only: list all third parties, optionally filtered by name.
     */
    @PreAuthorize("hasRole('ROLE_app_admin')")
    @GetMapping
    public List<ThirdParty> getThirdParties(@RequestParam(required = false) String name) {
        if (name != null && !name.isBlank())
            return thirdPartyRepository.findByNameContainingIgnoreCase(name);
        return thirdPartyRepository.findAll();
    }

    /**
     * GET /api/third-parties/{identifier}
     * Retrieve a single third party by its identifier (e.g. TP-001).
     */
    @GetMapping("/{identifier}")
    public ResponseEntity<ThirdParty> getThirdParty(@PathVariable String identifier) {
        return thirdPartyRepository.findByIdentifier(identifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/third-parties
     * Admin only: manually create a new third party via the REST API.
     * The identifier (TP-XXX) is auto-generated server-side.
     */
    @PreAuthorize("hasRole('ROLE_app_admin')")
    @PostMapping
    public ResponseEntity<?> createThirdParty(@RequestBody ThirdParty thirdParty) {
        // Validate required fields
        if (thirdParty.getName() == null || thirdParty.getName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Le champ 'name' est obligatoire."));
        if (thirdParty.getType() == null || thirdParty.getType().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Le champ 'type' est obligatoire (PHYSICAL_PERSON ou LEGAL_ENTITY)."));

        // Auto-generate identifier TP-XXX
        List<ThirdParty> all = thirdPartyRepository.findAll();
        int maxId = all.stream()
                .map(ThirdParty::getIdentifier)
                .filter(id -> id != null && id.startsWith("TP-"))
                .mapToInt(id -> {
                    try { return Integer.parseInt(id.substring(3)); }
                    catch (NumberFormatException e) { return 0; }
                })
                .max()
                .orElse(0);

        thirdParty.setIdentifier(String.format("TP-%03d", maxId + 1));

        // Normalize type to uppercase
        thirdParty.setType(thirdParty.getType().trim().toUpperCase());

        // Default country to FR if not set
        if (thirdParty.getCountry() == null || thirdParty.getCountry().isBlank())
            thirdParty.setCountry("FR");

        ThirdParty saved = thirdPartyRepository.save(thirdParty);
        return ResponseEntity.ok(saved);
    }
}