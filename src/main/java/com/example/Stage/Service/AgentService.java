package com.example.Stage.Service;

import com.example.Stage.Llm.LlmProviderRegistry;
import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.*;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentService {

    private final LlmProviderRegistry registry;
    private final ToolRegistry toolRegistry;

    public AgentService(LlmProviderRegistry registry, ToolRegistry toolRegistry) {
        this.registry = registry;
        this.toolRegistry = toolRegistry;
    }

    // ─────────────────────────────────────────────
    //  Auth helpers
    // ─────────────────────────────────────────────

    private boolean isUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_app_admin"));
    }

    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            Object username = jwt.getTokenAttributes().get("preferred_username");
            if (username != null) {
                return username.toString().toUpperCase();
            }
        }
        return "ANONYMOUS";
    }

    // ─────────────────────────────────────────────
    //  Main agent entry point
    // ─────────────────────────────────────────────

    public String handleMessage(String userMessage, String provider, List<LlmMessage> history) {
        LlmService llmService = registry.get(provider);
        String userId  = getAuthenticatedUserId();
        boolean isAdmin = isUserAdmin();
        String role    = isAdmin ? "ADMIN" : "CLIENT";

        // ── System prompt (formatted ONCE, no nested placeholders) ────────────
        String systemPrompt = """
            MANDATORY: Always reply in the SAME LANGUAGE as the user.
            If the user speaks French, reply in French.
            If the user speaks English, reply in English.

            Tu es l'Expert IA de la plateforme Solife de Vermeg, spécialisé en assurance vie.
            Utilisateur connecté : %s | Rôle : %s.

            ═══ RÈGLES GÉNÉRALES ═══
            - Les numéros de police sont au format '000-000' (ex: 001-042).
            - Les identifiants de tiers sont au format 'TP-000' (ex: TP-001).
            - Réponds toujours de façon concise, professionnelle et structurée.
            - En cas d'erreur d'un outil, explique-la clairement à l'utilisateur.

            ═══ OUTILS DISPONIBLES ═══
            1. searchPolicies        → Lister les polices (filtres: statut, souscripteur, produit)
            2. searchPoliciesByNumber → Rechercher par numéro de police
            3. getPolicyDetail       → Détail complet d'une police
            4. searchThirdParties    → Chercher un client/tiers par nom
            5. getPolicyRoles        → Rôles d'une police (bénéficiaire, assuré...)
            6. getPolicyCoverages    → Garanties d'une police
            7. getPolicyBills        → Factures d'une police
            8. createThirdParty      → Créer un nouveau client/tiers
            9. createSubscription    → Créer un nouveau contrat d'assurance

            ═══ FLUX DE CRÉATION ═══
            Pour créer un contrat pour un client NOUVEAU :
              Étape 1 → createThirdParty (créer le client, récupérer son identifiant TP-XXX)
              Étape 2 → createSubscription (utiliser l'identifiant TP-XXX obtenu)
            Pour créer un contrat pour un client EXISTANT :
              Étape 1 → searchThirdParties (trouver l'identifiant TP-XXX)
              Étape 2 → createSubscription

            ═══ RÈGLES DE SÉCURITÉ ═══
            - Les CLIENTs ne voient que leurs propres polices.
            - Les ADMINs voient toutes les polices.
            - Ne jamais inventer de données, utiliser uniquement les outils.

            ═══ COMMANDES UI ═══
            Actions que l'interface peut déclencher :
            - REFRESH_AND_FILTER (params: status, policyNumber)
            - OPEN_POLICY_DETAIL (params: policyNumber)
            - NAVIGATE (targetPage: policies, bills, tiers, coverages)
            """.formatted(userId, role);

        // ── Build message list ────────────────────────────────────────────────
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(systemPrompt));

        if (history != null && !history.isEmpty()) {
            // Keep last 4 messages to reduce token usage (Groq TPM limit)
            int start = Math.max(0, history.size() - 4);
            messages.addAll(history.subList(start, history.size()));
        }

        messages.add(LlmMessage.user(userMessage));

        // ── Guard flags ───────────────────────────────────────────────────────
        boolean subscriptionDone  = false;
        boolean thirdPartyDone    = false;

        // ── Agent reasoning loop (max 4 iterations to limit token usage) ──────
        for (int i = 0; i < 4; i++) {
            LlmResponse response = llmService.askWithTools(messages, toolRegistry.getTools(), LlmConfig.defaults());

            if (response.hasToolCalls()) {
                // Add the assistant's tool-call message to the conversation
                String assistantText = response.text() != null ? response.text() : "";
                messages.add(new LlmMessage("assistant", assistantText, response.toolCalls(), null));

                // Execute each requested tool
                for (LlmToolCall toolCall : response.toolCalls()) {
                    System.out.printf("[AGENT] Tool called: [%s] | Input: %s%n",
                            toolCall.name(), toolCall.input());

                    // ── Anti-duplicate guards ─────────────────────────────────
                    if (toolCall.name().equals("createSubscription") && subscriptionDone) {
                        messages.add(LlmMessage.toolResult(toolCall.id(),
                                "ERREUR: Ce contrat a déjà été créé. Ne pas recommencer."));
                        continue;
                    }
                    if (toolCall.name().equals("createThirdParty") && thirdPartyDone) {
                        messages.add(LlmMessage.toolResult(toolCall.id(),
                                "ERREUR: Ce tiers a déjà été créé. Utilisez l'identifiant retourné précédemment."));
                        continue;
                    }

                    // ── Execute tool ──────────────────────────────────────────
                    String result;
                    try {
                        result = toolRegistry.executeTool(toolCall.name(), toolCall.input());
                    } catch (Exception e) {
                        result = "Erreur technique lors de l'exécution de l'outil : " + e.getMessage();
                    }

                    // Set guard flags on success
                    if (toolCall.name().equals("createSubscription") && result.contains("SUCCESS")) {
                        subscriptionDone = true;
                    }
                    if (toolCall.name().equals("createThirdParty") && result.contains("SUCCESS_THIRD_PARTY")) {
                        thirdPartyDone = true;
                    }

                    messages.add(LlmMessage.toolResult(toolCall.id(), result));
                }

                // If both creation steps are done, force the final response immediately
                if (subscriptionDone) {
                    return llmService.ask(messages, LlmConfig.defaults());
                }

            } else {
                // No tool calls → this is the final textual answer
                return response.text();
            }
        }

        return "Désolé, la recherche a pris trop de temps. " +
               "Veuillez vérifier votre tableau de bord pour voir si l'action a été effectuée.";
    }
}