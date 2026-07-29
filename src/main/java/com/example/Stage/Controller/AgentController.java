package com.example.Stage.Controller;

import com.example.Stage.Llm.LlmProviderRegistry;
import com.example.Stage.Llm.Model.LlmMessage;
import com.example.Stage.Service.AgentService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class AgentController {

    private final LlmProviderRegistry registry;
    private final AgentService agentService;

    public AgentController(LlmProviderRegistry registry, AgentService agentService) {
        this.registry = registry;
        this.agentService = agentService;
    }

    public record AgentRequest(String provider, List<LlmMessage> messages) {}
    public record AgentResponse(String response, String provider, UiCommand command) {}
    public record UiCommand(String action, String targetPage, Map<String, String> params) {}

    @PostMapping("/agent")
    public AgentResponse chat(@RequestBody AgentRequest request) {
        String provider = request.provider() != null ? request.provider() : "groq";
        List<LlmMessage> allMessages = request.messages();

        if (allMessages == null || allMessages.isEmpty())
            return new AgentResponse("Historique vide.", provider, null);

        LlmMessage last = allMessages.get(allMessages.size() - 1);
        String userContent = last.content();
        List<LlmMessage> history = allMessages.subList(0, allMessages.size() - 1);

        String aiResponse = agentService.handleMessage(userContent, provider, history);
        UiCommand uiCommand = detectUiCommand(userContent, aiResponse);

        return new AgentResponse(aiResponse, provider, uiCommand);
    }

    @GetMapping("/providers")
    public Set<String> listProviders() {
        return registry.getAll().keySet();
    }

    // ─────────────────────────────────────────────
    //  UI Command detection
    // ─────────────────────────────────────────────

    private UiCommand detectUiCommand(String userMessage, String aiResponse) {
        if (userMessage == null || aiResponse == null) return null;

        String userMsg = userMessage.toLowerCase();
        String aiMsg   = aiResponse.toLowerCase();
        Map<String, String> params = new HashMap<>();

        // Extract policy number from AI response first (most reliable), then from user message
        String policyNumber = extractPolicyNumber(aiMsg);
        if (policyNumber == null) policyNumber = extractPolicyNumber(userMsg);

        // ── 1. New third party created ─────────────────────────────────────
        if (aiResponse.contains("SUCCESS_THIRD_PARTY")) {
            return new UiCommand("NAVIGATE", "third-parties", params);
        }

        // ── 2. New policy created ──────────────────────────────────────────
        if (aiResponse.contains("SUCCESS")) {
            if (policyNumber != null) params.put("policyNumber", policyNumber);
            return new UiCommand("REFRESH_AND_FILTER", "policies", params);
        }

        // ── 3. Open policy detail ──────────────────────────────────────────
        if (policyNumber != null && containsAny(userMsg,
                "détail", "detail", "ouvrir", "voir", "consulter", "show", "open", "display")) {
            params.put("policyNumber", policyNumber);
            return new UiCommand("OPEN_POLICY_DETAIL", "policies", params);
        }

        // ── 4. Navigate to bills ───────────────────────────────────────────
        if (containsAny(userMsg, "facture", "paye", "échéance", "bill", "invoice", "payment")) {
            if (containsAny(userMsg, "impayé", "unpaid")) params.put("status", "UNPAID");
            else if (containsAny(userMsg, "payé", "paid"))  params.put("status", "PAID");
            else if (containsAny(userMsg, "en attente", "pending")) params.put("status", "PENDING");
            return new UiCommand("NAVIGATE", "bills", params);
        }

        // ── 5. Navigate to third parties ───────────────────────────────────
        if (containsAny(userMsg, "tiers", "client", "personne", "assuré", "souscripteur",
                "third party", "customer", "beneficiaire", "bénéficiaire")) {
            return new UiCommand("NAVIGATE", "third-parties", params);
        }

        // ── 6. Navigate to coverages ───────────────────────────────────────
        if (containsAny(userMsg, "garantie", "couverture", "risque", "coverage", "guarantee")) {
            return new UiCommand("NAVIGATE", "coverages", params);
        }

        // ── 7. Navigate to endorsements ────────────────────────────────────
        if (containsAny(userMsg, "avenant", "modification", "dossier", "endorsement")) {
            return new UiCommand("NAVIGATE", "endorsements", params);
        }

        // ── 8. Filter / search policies ────────────────────────────────────
        if (containsAny(userMsg, "police", "contrat", "policy", "contract") || policyNumber != null) {
            if (containsAny(userMsg, "active", "actif"))           params.put("status", "ACTIVE");
            else if (containsAny(userMsg, "annulé", "cancelled"))  params.put("status", "CANCELLED");
            else if (containsAny(userMsg, "suspendu", "suspended"))params.put("status", "SUSPENDED");
            else if (containsAny(userMsg, "en cours", "pending"))  params.put("status", "PENDING");

            if (policyNumber != null) params.put("policyNumber", policyNumber);
            return new UiCommand("REFRESH_AND_FILTER", "policies", params);
        }

        return null;
    }

    /**
     * Extract a policy number matching the strict format 000-000 from a given text.
     */
    private String extractPolicyNumber(String text) {
        if (text == null) return null;
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\\b\\d{3}-\\d{3}\\b").matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Returns true if the given text contains any of the provided keywords.
     */
    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}