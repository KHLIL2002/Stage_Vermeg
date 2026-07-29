package com.example.Stage.Service;

import com.example.Stage.Llm.Model.LlmToolDefinition;
import com.example.Stage.Model.*;
import com.example.Stage.Repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class ToolRegistry {

    private final PolicyRepository policyRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final PolicyRoleRepository policyRoleRepository;
    private final CoverageRepository coverageRepository;
    private final BillRepository billRepository;
    private final ProductRepository productRepository;
    private final EndorsementRepository endorsementRepository;

    public ToolRegistry(PolicyRepository policyRepository,
                        ThirdPartyRepository thirdPartyRepository,
                        PolicyRoleRepository policyRoleRepository,
                        CoverageRepository coverageRepository,
                        BillRepository billRepository,
                        ProductRepository productRepository,
                        EndorsementRepository endorsementRepository) {
        this.policyRepository = policyRepository;
        this.thirdPartyRepository = thirdPartyRepository;
        this.policyRoleRepository = policyRoleRepository;
        this.coverageRepository = coverageRepository;
        this.billRepository = billRepository;
        this.productRepository = productRepository;
        this.endorsementRepository = endorsementRepository;
    }

    // ─────────────────────────────────────────────
    //  Auth helpers
    // ─────────────────────────────────────────────

    /**
     * Returns the preferred_username (uppercased) from the JWT token.
     * This matches the identifier format used in the DataLoader (e.g. "TP-001").
     */
    /** Retourne la valeur (trimée) de la clé si présente et non vide, sinon la valeur par défaut. */
    private String optStr(Map<String, Object> input, String key, String def) {
        Object v = input.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString().trim() : def;
    }

    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            Object username = jwt.getTokenAttributes().get("preferred_username");
            if (username != null) return username.toString().toUpperCase();
        }
        return null;
    }

    /**
     * Returns true if the authenticated user has the ROLE_app_admin authority.
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_app_admin"));
    }

    /**
     * Vérifie que l'utilisateur connecté a le droit de consulter cette police.
     * Un ADMIN peut accéder à toutes les polices ; un CLIENT uniquement aux siennes.
     *
     * @return null si l'accès est autorisé, sinon un message d'erreur à renvoyer.
     */
    private String checkPolicyAccess(String policyNumber) {
        if (policyNumber == null || policyNumber.isBlank())
            return "❌ Erreur : le numéro de police est obligatoire.";

        if (isAdmin()) {
            return policyRepository.findByPolicyNumber(policyNumber).isPresent()
                    ? null
                    : "❌ Police introuvable : " + policyNumber + ".";
        }

        String username = getAuthenticatedUserId();
        return policyRepository.findByPolicyNumberAndOwner(policyNumber, username).isPresent()
                ? null
                : "❌ Police introuvable ou non autorisée : " + policyNumber +
                  ". Vérifiez le numéro ou que cette police vous appartient.";
    }

    // ─────────────────────────────────────────────
    //  Tool definitions exposed to the LLM
    // ─────────────────────────────────────────────

    public List<LlmToolDefinition> getTools() {
        return List.of(

                // ── 1. Search policies ──────────────────────────────────────────────
                new LlmToolDefinition(
                        "searchPolicies",
                        "Rechercher des polices d'assurance. Peut filtrer par statut, par nom du souscripteur, ou par code produit. Sans filtre, retourne toutes les polices de l'utilisateur connecté.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "status", Map.of("type", "string",
                                                "description", "Statut de la police",
                                                "enum", List.of("ACTIVE", "CANCELLED", "SUSPENDED", "PENDING")),
                                        "holderName", Map.of("type", "string",
                                                "description", "Nom du souscripteur (recherche partielle)"),
                                        "productCode", Map.of("type", "string",
                                                "description", "Code produit exact (ex: EPARGNE_VIE, TEMPO_DECES)")
                                ),
                                "required", List.of()
                        )
                ),

                // ── 2. Search by policy number ──────────────────────────────────────
                new LlmToolDefinition(
                        "searchPoliciesByNumber",
                        "Rechercher des polices par leur numéro (complet ou partiel). Format attendu: 000-000.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "policyNumber", Map.of("type", "string",
                                                "description", "Numéro de police (ex: 001-003)")
                                ),
                                "required", List.of("policyNumber")
                        )
                ),

                // ── 3. Get policy detail ────────────────────────────────────────────
                new LlmToolDefinition(
                        "getPolicyDetail",
                        "Obtenir tous les détails d'une police spécifique : produit, statut, prime annuelle, dates, souscripteur, garanties, factures.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "policyNumber", Map.of("type", "string",
                                                "description", "Le numéro exact de la police (ex: 001-001)")
                                ),
                                "required", List.of("policyNumber")
                        )
                ),

                // ── 4. Search third parties ─────────────────────────────────────────
                new LlmToolDefinition(
                        "searchThirdParties",
                        "Rechercher des clients ou bénéficiaires par nom. Retourne l'identifiant (ex: TP-001) à réutiliser pour créer une souscription.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "name", Map.of("type", "string",
                                                "description", "Nom du tiers (recherche partielle)")
                                ),
                                "required", List.of("name")
                        )
                ),

                // ── 5. Get policy roles ─────────────────────────────────────────────
                new LlmToolDefinition(
                        "getPolicyRoles",
                        "Obtenir les rôles liés à une police (souscripteur, bénéficiaire, assuré, payeur).",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "policyNumber", Map.of("type", "string",
                                                "description", "Le numéro de la police")
                                ),
                                "required", List.of("policyNumber")
                        )
                ),

                // ── 6. Get policy coverages ─────────────────────────────────────────
                new LlmToolDefinition(
                        "getPolicyCoverages",
                        "Obtenir les garanties d'une police (capital assuré, type de garantie, statut).",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "policyNumber", Map.of("type", "string",
                                                "description", "Le numéro de la police")
                                ),
                                "required", List.of("policyNumber")
                        )
                ),

                // ── 7. Get policy bills ─────────────────────────────────────────────
                new LlmToolDefinition(
                        "getPolicyBills",
                        "Obtenir les factures d'une police, avec filtre optionnel par statut.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "policyNumber", Map.of("type", "string",
                                                "description", "Le numéro de la police"),
                                        "status", Map.of("type", "string",
                                                "description", "Statut des factures",
                                                "enum", List.of("PAID", "PENDING", "UNPAID"))
                                ),
                                "required", List.of("policyNumber")
                        )
                ),

                // ── 8. Create subscription ──────────────────────────────────────────
                new LlmToolDefinition(
                        "createSubscription",
                        "Initier une nouvelle souscription de contrat d'assurance. Nécessite un code produit valide, l'identifiant d'un tiers existant (ex: TP-001), et le montant de la prime annuelle. Si le tiers n'existe pas, utiliser d'abord 'createThirdParty'.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "productCode", Map.of("type", "string",
                                                "description", "Code produit (ex: EPARGNE_VIE, TEMPO_DECES, MULTI_SUPPORT, PER_INDIVIDUEL)"),
                                        "holderId", Map.of("type", "string",
                                                "description", "Identifiant du tiers souscripteur (ex: TP-001)"),
                                        "premium", Map.of("type", "number",
                                                "description", "Montant de la prime annuelle en EUR")
                                ),
                                "required", List.of("productCode", "holderId", "premium")
                        )
                ),

                // ── 9. Create third party ───────────────────────────────────────────
                new LlmToolDefinition(
                        "createThirdParty",
                        "Créer un nouveau tiers (client) dans le système. Utiliser cet outil avant 'createSubscription' si le client n'existe pas encore. Retourne l'identifiant du tiers créé (ex: TP-036).",
                        Map.of(
                                "type", "object",
                                // NOTE: Map.of ne supporte que 10 paires clé/valeur au maximum.
                                // Ce schéma a 11 propriétés → on utilise Map.ofEntries (sinon: ne compile pas).
                                "properties", Map.ofEntries(
                                        Map.entry("type", Map.of("type", "string",
                                                "description", "Type de tiers",
                                                "enum", List.of("PHYSICAL_PERSON", "LEGAL_ENTITY"))),
                                        Map.entry("name", Map.of("type", "string",
                                                "description", "Nom de famille (personne physique) ou raison sociale (personne morale)")),
                                        Map.entry("firstName", Map.of("type", "string",
                                                "description", "Prénom (uniquement pour PHYSICAL_PERSON)")),
                                        Map.entry("birthDate", Map.of("type", "string",
                                                "description", "Date de naissance au format YYYY-MM-DD (uniquement pour PHYSICAL_PERSON)")),
                                        Map.entry("gender", Map.of("type", "string",
                                                "description", "Genre",
                                                "enum", List.of("MALE", "FEMALE"))),
                                        Map.entry("email", Map.of("type", "string",
                                                "description", "Adresse email")),
                                        Map.entry("phone", Map.of("type", "string",
                                                "description", "Numéro de téléphone")),
                                        Map.entry("street", Map.of("type", "string",
                                                "description", "Rue et numéro")),
                                        Map.entry("postalCode", Map.of("type", "string",
                                                "description", "Code postal")),
                                        Map.entry("town", Map.of("type", "string",
                                                "description", "Ville")),
                                        Map.entry("country", Map.of("type", "string",
                                                "description", "Code pays ISO 2 lettres (ex: FR, TN, BE)"))
                                ),
                                "required", List.of("type", "name")
                        )
                )
        );
    }

    // ─────────────────────────────────────────────
    //  Tool dispatcher
    // ─────────────────────────────────────────────

    public String executeTool(String name, Map<String, Object> input) {
        try {
            return switch (name) {
                case "searchPolicies"        -> executeSearchPolicies(input);
                case "searchPoliciesByNumber"-> executeSearchPoliciesByNumber(input);
                case "getPolicyDetail"       -> executeGetPolicyDetail(input);
                case "searchThirdParties"    -> executeSearchThirdParties(input);
                case "getPolicyRoles"        -> executeGetPolicyRoles(input);
                case "getPolicyCoverages"    -> executeGetPolicyCoverages(input);
                case "getPolicyBills"        -> executeGetPolicyBills(input);
                case "createSubscription"    -> executeCreateSubscription(input);
                case "createThirdParty"      -> executeCreateThirdParty(input);
                default                      -> "Outil inconnu : " + name;
            };
        } catch (Exception e) {
            return "Erreur lors de l'exécution de l'outil '" + name + "' : " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────
    //  Tool implementations
    // ─────────────────────────────────────────────

    /**
     * FIX: Filter by authenticated user by default.
     * Admins see all policies; regular users only see their own.
     * Additional filters (status, holderName, productCode) are applied on top.
     */
    private String executeSearchPolicies(Map<String, Object> input) {
        String username = getAuthenticatedUserId();
        boolean admin = isAdmin();

        List<Policy> policies;

        if (input.containsKey("status")) {
            String status = (String) input.get("status");
            policies = admin
                    ? policyRepository.findByStatus(status)
                    : policyRepository.findByStatusAndHolderIdentifier(status, username);

        } else if (input.containsKey("holderName")) {
            String holderName = (String) input.get("holderName");
            policies = admin
                    ? policyRepository.findByHolderNameContainingIgnoreCase(holderName)
                    : policyRepository.findByHolderNameContainingIgnoreCaseAndHolderIdentifier(holderName, username);

        } else if (input.containsKey("productCode")) {
            String productCode = (String) input.get("productCode");
            policies = admin
                    ? policyRepository.findByProductProductCode(productCode)
                    : policyRepository.findByProductProductCodeAndHolderIdentifier(productCode, username);

        } else {
            // No filter: return user's own policies (all for admin)
            policies = admin
                    ? policyRepository.findAll()
                    : policyRepository.findByHolderIdentifier(username);
        }

        if (policies.isEmpty()) return "Aucune police trouvée.";

        int total = policies.size();
        List<Policy> displayed = total > 20 ? policies.subList(0, 20) : policies;

        StringBuilder sb = new StringBuilder();
        sb.append("Polices trouvées : ").append(total);
        if (total > 20) sb.append(" (affichage des 20 premières)");
        sb.append(" :\n\n");

        for (Policy p : displayed) {
            sb.append("• ").append(p.getPolicyNumber())
              .append(" | ").append(p.getProduct().getName())
              .append(" | Statut: ").append(p.getStatus())
              .append(" | Souscripteur: ").append(p.getHolder().getName());
            if (p.getHolder().getFirstName() != null) sb.append(" ").append(p.getHolder().getFirstName());
            sb.append(" | Prime: ").append(p.getAnnualPremium()).append(" EUR");
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * FIX: Return real, complete policy details instead of a placeholder string.
     * Admins can see any policy; clients only their own.
     */
    private String executeGetPolicyDetail(Map<String, Object> input) {
        String policyNumber = ((String) input.get("policyNumber")).trim();
        String username = getAuthenticatedUserId();
        boolean admin = isAdmin();

        Policy p;
        if (admin) {
            p = policyRepository.findByPolicyNumber(policyNumber)
                    .orElse(null);
        } else {
            p = policyRepository.findByPolicyNumberAndOwner(policyNumber, username)
                    .orElse(null);
        }

        if (p == null) {
            return "❌ Police introuvable : " + policyNumber +
                   (admin ? "." : ". Vérifiez le numéro ou que cette police vous appartient.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("━━━ DÉTAIL DE LA POLICE ").append(p.getPolicyNumber()).append(" ━━━\n\n");

        // General info
        sb.append("📋 **Informations générales**\n");
        sb.append("  • Produit       : ").append(p.getProduct().getName())
          .append(" (").append(p.getProduct().getProductCode()).append(")\n");
        sb.append("  • Statut        : ").append(p.getStatus()).append("\n");
        sb.append("  • Devise        : ").append(p.getCurrency()).append("\n");
        sb.append("  • Date d'effet  : ").append(p.getEffectiveDate()).append("\n");
        if (p.getTermDate() != null)
            sb.append("  • Date de fin   : ").append(p.getTermDate()).append("\n");

        // Financial info
        sb.append("\n💰 **Informations financières**\n");
        sb.append("  • Prime annuelle     : ").append(p.getAnnualPremium()).append(" ").append(p.getCurrency()).append("\n");
        if (p.getSurrenderValue() != null)
            sb.append("  • Valeur de rachat   : ").append(p.getSurrenderValue()).append(" ").append(p.getCurrency()).append("\n");
        if (p.getPaymentPeriodicity() != null)
            sb.append("  • Périodicité paiement : ").append(p.getPaymentPeriodicity()).append("\n");
        if (p.getPaymentMode() != null)
            sb.append("  • Mode de paiement   : ").append(p.getPaymentMode()).append("\n");
        if (p.getPremiumType() != null)
            sb.append("  • Type de prime      : ").append(p.getPremiumType()).append("\n");

        // Holder info
        ThirdParty h = p.getHolder();
        sb.append("\n👤 **Souscripteur**\n");
        sb.append("  • Identifiant : ").append(h.getIdentifier()).append("\n");
        sb.append("  • Nom         : ").append(h.getName());
        if (h.getFirstName() != null) sb.append(" ").append(h.getFirstName());
        sb.append("\n");
        if (h.getEmail() != null)  sb.append("  • Email       : ").append(h.getEmail()).append("\n");
        if (h.getPhone() != null)  sb.append("  • Téléphone   : ").append(h.getPhone()).append("\n");
        if (h.getTown() != null)   sb.append("  • Ville       : ").append(h.getTown()).append("\n");

        // Coverages summary
        List<Coverage> coverages = coverageRepository.findByPolicyPolicyNumber(policyNumber);
        if (!coverages.isEmpty()) {
            sb.append("\n🛡️ **Garanties (").append(coverages.size()).append(")**\n");
            for (Coverage c : coverages) {
                sb.append("  • ").append(c.getLabel())
                  .append(" | Capital: ").append(c.getCapital()).append(" EUR")
                  .append(" | Statut: ").append(c.getStatus()).append("\n");
            }
        }

        // Bills summary
        List<Bill> bills = billRepository.findByPolicyPolicyNumber(policyNumber);
        if (!bills.isEmpty()) {
            long unpaid = bills.stream().filter(b -> "UNPAID".equals(b.getStatus())).count();
            long pending = bills.stream().filter(b -> "PENDING".equals(b.getStatus())).count();
            sb.append("\n🧾 **Factures (").append(bills.size()).append(" au total)**\n");
            sb.append("  • Impayées : ").append(unpaid).append("\n");
            sb.append("  • En attente : ").append(pending).append("\n");
        }

        return sb.toString();
    }

    private String executeSearchThirdParties(Map<String, Object> input) {
        String name = (String) input.get("name");
        List<ThirdParty> tps = thirdPartyRepository.findByNameContainingIgnoreCase(name);
        if (tps.isEmpty()) return "Aucun tiers trouvé pour le nom : " + name;

        StringBuilder sb = new StringBuilder("Tiers trouvés (" + tps.size() + ") :\n\n");
        for (ThirdParty tp : tps) {
            sb.append("• ").append(tp.getIdentifier())
              .append(" | ").append(tp.getName());
            if (tp.getFirstName() != null) sb.append(" ").append(tp.getFirstName());
            sb.append(" | Type: ").append(tp.getType());
            if (tp.getTown() != null) sb.append(" | Ville: ").append(tp.getTown());
            if (tp.getEmail() != null) sb.append(" | Email: ").append(tp.getEmail());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String executeGetPolicyRoles(Map<String, Object> input) {
        String policyNumber = (String) input.get("policyNumber");
        if (policyNumber != null) policyNumber = policyNumber.trim();

        String access = checkPolicyAccess(policyNumber);
        if (access != null) return access;

        List<PolicyRole> roles = policyRoleRepository.findByPolicyPolicyNumber(policyNumber);
        if (roles.isEmpty()) return "Aucun rôle trouvé pour la police : " + policyNumber;

        StringBuilder sb = new StringBuilder("Rôles pour la police " + policyNumber + " :\n\n");
        for (PolicyRole r : roles) {
            sb.append("• ").append(r.getRoleType())
              .append(" : ").append(r.getThirdParty().getName());
            if (r.getThirdParty().getFirstName() != null)
                sb.append(" ").append(r.getThirdParty().getFirstName());
            sb.append(" (").append(r.getThirdParty().getIdentifier()).append(")");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String executeGetPolicyCoverages(Map<String, Object> input) {
        String policyNumber = (String) input.get("policyNumber");
        if (policyNumber != null) policyNumber = policyNumber.trim();

        String access = checkPolicyAccess(policyNumber);
        if (access != null) return access;

        List<Coverage> coverages = coverageRepository.findByPolicyPolicyNumber(policyNumber);
        if (coverages.isEmpty()) return "Aucune garantie trouvée pour la police : " + policyNumber;

        StringBuilder sb = new StringBuilder("Garanties pour la police " + policyNumber + " :\n\n");
        for (Coverage c : coverages) {
            sb.append("• ").append(c.getLabel())
              .append(" | Type: ").append(c.getCoverageType())
              .append(" | Capital: ").append(c.getCapital()).append(" EUR")
              .append(" | Prime: ").append(c.getPremium()).append(" EUR")
              .append(" | Statut: ").append(c.getStatus())
              .append(c.isMainCover() ? " | [PRINCIPALE]" : "")
              .append("\n");
        }
        return sb.toString();
    }

    private String executeGetPolicyBills(Map<String, Object> input) {
        String policyNumber = (String) input.get("policyNumber");
        if (policyNumber != null) policyNumber = policyNumber.trim();

        String access = checkPolicyAccess(policyNumber);
        if (access != null) return access;

        List<Bill> bills = billRepository.findByPolicyPolicyNumber(policyNumber);

        if (input.containsKey("status")) {
            String status = (String) input.get("status");
            bills = bills.stream().filter(b -> status.equalsIgnoreCase(b.getStatus())).toList();
        }

        if (bills.isEmpty()) return "Aucune facture trouvée pour la police : " + policyNumber;

        StringBuilder sb = new StringBuilder("Factures pour la police " + policyNumber + " (" + bills.size() + ") :\n\n");
        for (Bill b : bills) {
            sb.append("• ").append(b.getIdentifier())
              .append(" | Montant: ").append(b.getBillAmount()).append(" EUR")
              .append(" | Statut: ").append(b.getStatus());
            if (b.getEffectiveDate() != null)
                sb.append(" | Date: ").append(b.getEffectiveDate());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String executeSearchPoliciesByNumber(Map<String, Object> input) {
        Object val = input.get("policyNumber");
        if (val == null) return "Erreur : numéro de police manquant.";

        String policyNumber = val.toString().trim();
        List<Policy> policies = policyRepository.findByPolicyNumberContainingIgnoreCase(policyNumber);

        if (policies.isEmpty()) {
            return "Aucune police trouvée contenant '" + policyNumber + "'. " +
                   "Vérifiez le format (ex: 002-001).";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Police(s) correspondant à '").append(policyNumber).append("' (")
          .append(policies.size()).append(") :\n\n");

        policies.stream().limit(20).forEach(p -> {
            sb.append("• ").append(p.getPolicyNumber())
              .append(" | ").append(p.getHolder().getName());
            if (p.getHolder().getFirstName() != null) sb.append(" ").append(p.getHolder().getFirstName());
            sb.append(" | ").append(p.getProduct().getName())
              .append(" | Statut: ").append(p.getStatus())
              .append("\n");
        });

        return sb.toString();
    }

    private String executeCreateSubscription(Map<String, Object> input) {
        // 1. Extract parameters
        String pCode = (String) input.get("productCode");
        String hId   = (String) input.get("holderId");

        if (pCode == null || pCode.isBlank())
            return "❌ Erreur : le code produit est obligatoire.";
        if (hId == null || hId.isBlank())
            return "❌ Erreur : l'identifiant du tiers est obligatoire.";

        BigDecimal premium = BigDecimal.ZERO;
        if (input.get("premium") != null) {
            try {
                premium = new BigDecimal(input.get("premium").toString());
            } catch (NumberFormatException e) {
                return "❌ Erreur : la prime doit être un nombre valide.";
            }
        }

        // 2. Find product
        Product product = productRepository.findByProductCode(pCode.trim().toUpperCase());
        if (product == null) {
            return "❌ Erreur : Le code produit '" + pCode + "' n'existe pas. " +
                   "Produits disponibles : EPARGNE_VIE, MULTI_SUPPORT, TEMPO_DECES, PER_INDIVIDUEL, " +
                   "CAPITALISATION, VIE_ENTIERE, PREVOYANCE, RETRAITE_COL.";
        }

        // 3. Find holder
        ThirdParty holder = thirdPartyRepository.findByIdentifier(hId.trim().toUpperCase()).orElse(null);
        if (holder == null) {
            return "❌ Erreur : Le tiers '" + hId + "' est introuvable. " +
                   "Utilisez 'searchThirdParties' pour trouver l'identifiant ou " +
                   "'createThirdParty' pour créer un nouveau client.";
        }

        // 4. Generate the NEXT sequential policy number.
        //    Format XXX-YYY : YYY va de 001 à 010, puis on passe au groupe suivant.
        //    Exemple : après 022-010, le prochain contrat est 023-001.
        int maxGroup = 0;
        int maxSub = 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d{3})-(\\d{3})$");
        for (Policy existing : policyRepository.findAll()) {
            String pn = existing.getPolicyNumber();
            if (pn == null) continue;
            java.util.regex.Matcher m = pattern.matcher(pn.trim());
            if (!m.matches()) continue;
            int g = Integer.parseInt(m.group(1));
            int s = Integer.parseInt(m.group(2));
            if (g > maxGroup) { maxGroup = g; maxSub = s; }
            else if (g == maxGroup && s > maxSub) { maxSub = s; }
        }

        int nextGroup;
        int nextSub;
        if (maxGroup == 0) {
            nextGroup = 1; nextSub = 1;            // aucune police existante
        } else if (maxSub >= 10) {
            nextGroup = maxGroup + 1; nextSub = 1;  // groupe complet → groupe suivant
        } else {
            nextGroup = maxGroup; nextSub = maxSub + 1;
        }

        String policyNumber = String.format("%03d-%03d", nextGroup, nextSub);

        // Sécurité : en cas de collision improbable, on avance jusqu'au prochain numéro libre.
        while (policyRepository.findByPolicyNumber(policyNumber).isPresent()) {
            if (nextSub >= 10) { nextGroup++; nextSub = 1; } else { nextSub++; }
            policyNumber = String.format("%03d-%03d", nextGroup, nextSub);
        }

        // 5. Create and save the policy
        Policy newPolicy = new Policy();
        newPolicy.setPolicyNumber(policyNumber);
        newPolicy.setProduct(product);
        newPolicy.setHolder(holder);
        newPolicy.setStatus("PENDING");
        newPolicy.setAnnualPremium(premium);

        // Date d'effet : celle fournie par le formulaire, sinon aujourd'hui
        LocalDate effectiveDate = LocalDate.now();
        Object effRaw = input.get("effectiveDate");
        if (effRaw != null && !effRaw.toString().isBlank()) {
            try {
                effectiveDate = LocalDate.parse(effRaw.toString().trim());
            } catch (DateTimeParseException e) {
                return "❌ Erreur : date d'effet invalide. Format attendu : YYYY-MM-DD.";
            }
        }
        newPolicy.setEffectiveDate(effectiveDate);

        // Date d'expiration (optionnelle)
        Object termRaw = input.get("termDate");
        if (termRaw != null && !termRaw.toString().isBlank()) {
            try {
                newPolicy.setTermDate(LocalDate.parse(termRaw.toString().trim()));
            } catch (DateTimeParseException e) {
                return "❌ Erreur : date d'expiration invalide. Format attendu : YYYY-MM-DD.";
            }
        }

        // Champs optionnels (valeurs choisies dans le formulaire)
        newPolicy.setCurrency(optStr(input, "currency", "EUR"));
        newPolicy.setTaxCountry(optStr(input, "taxCountry", "FR"));

        String payMode = optStr(input, "paymentMode", null);
        if (payMode != null) newPolicy.setPaymentMode(payMode);

        String periodicity = optStr(input, "paymentPeriodicity", null);
        if (periodicity != null) newPolicy.setPaymentPeriodicity(periodicity);

        String premiumType = optStr(input, "premiumType", null);
        if (premiumType != null) newPolicy.setPremiumType(premiumType);

        policyRepository.save(newPolicy);

        // Créer automatiquement le rôle HOLDER (souscripteur) pour cohérence
        // avec les polices existantes (sinon getPolicyRoles ne retourne rien).
        PolicyRole holderRole = new PolicyRole();
        holderRole.setPolicy(newPolicy);
        holderRole.setThirdParty(holder);
        holderRole.setRoleType("HOLDER");
        policyRoleRepository.save(holderRole);

        return "SUCCESS: Police créée avec succès. " +
               "NUMÉRO GÉNÉRÉ: " + policyNumber + ". " +
               "Produit: " + product.getName() + ". " +
               "Souscripteur: " + holder.getName() + (holder.getFirstName() != null ? " " + holder.getFirstName() : "") + ". " +
               "Prime annuelle: " + premium + " EUR. " +
               "Statut: PENDING. ACTION TERMINÉE.";
    }

    /**
     * NEW TOOL: Create a new third party (physical person or legal entity).
     * Returns the generated identifier (e.g. TP-036) for immediate reuse.
     */
    private String executeCreateThirdParty(Map<String, Object> input) {
        // 1. Validate required fields
        String type = (String) input.get("type");
        String name = (String) input.get("name");

        if (type == null || type.isBlank())
            return "❌ Erreur : le type de tiers est obligatoire (PHYSICAL_PERSON ou LEGAL_ENTITY).";
        if (name == null || name.isBlank())
            return "❌ Erreur : le nom est obligatoire.";

        type = type.trim().toUpperCase();
        if (!type.equals("PHYSICAL_PERSON") && !type.equals("LEGAL_ENTITY"))
            return "❌ Erreur : type invalide. Utilisez PHYSICAL_PERSON ou LEGAL_ENTITY.";

        // 2. Generate a unique identifier (TP-XXX)
        // Find the current max identifier number
        List<ThirdParty> all = thirdPartyRepository.findAll();
        int maxId = all.stream()
                .map(ThirdParty::getIdentifier)
                .filter(id -> id != null && id.startsWith("TP-"))
                .mapToInt(id -> {
                    try { return Integer.parseInt(id.substring(3)); }
                    catch (NumberFormatException ex) { return 0; }
                })
                .max()
                .orElse(0);

        String identifier = String.format("TP-%03d", maxId + 1);

        // 3. Build the ThirdParty entity
        ThirdParty tp = new ThirdParty();
        tp.setIdentifier(identifier);
        tp.setType(type);
        tp.setName(name.trim());

        // Optional fields
        if (input.containsKey("firstName") && input.get("firstName") != null)
            tp.setFirstName(input.get("firstName").toString().trim());

        if (input.containsKey("gender") && input.get("gender") != null)
            tp.setGender(input.get("gender").toString().trim().toUpperCase());

        if (input.containsKey("birthDate") && input.get("birthDate") != null) {
            try {
                tp.setBirthDate(LocalDate.parse(input.get("birthDate").toString().trim()));
            } catch (DateTimeParseException e) {
                return "❌ Erreur : date de naissance invalide. Format attendu : YYYY-MM-DD.";
            }
        }

        if (input.containsKey("email") && input.get("email") != null)
            tp.setEmail(input.get("email").toString().trim());

        if (input.containsKey("phone") && input.get("phone") != null)
            tp.setPhone(input.get("phone").toString().trim());

        if (input.containsKey("street") && input.get("street") != null)
            tp.setStreet(input.get("street").toString().trim());

        if (input.containsKey("postalCode") && input.get("postalCode") != null)
            tp.setPostalCode(input.get("postalCode").toString().trim());

        if (input.containsKey("town") && input.get("town") != null)
            tp.setTown(input.get("town").toString().trim());

        // Country defaults to "FR" if not provided
        String country = (input.containsKey("country") && input.get("country") != null)
                ? input.get("country").toString().trim().toUpperCase()
                : "FR";
        tp.setCountry(country);

        // 4. Persist
        thirdPartyRepository.save(tp);

        // 5. Build success response
        StringBuilder sb = new StringBuilder();
        sb.append("SUCCESS_THIRD_PARTY: Tiers créé avec succès. ");
        sb.append("IDENTIFIANT GÉNÉRÉ: ").append(identifier).append(". ");
        sb.append("Nom: ").append(tp.getName());
        if (tp.getFirstName() != null) sb.append(" ").append(tp.getFirstName());
        sb.append(". Type: ").append(type).append(". ");
        if (tp.getTown() != null) sb.append("Ville: ").append(tp.getTown()).append(". ");
        sb.append("Vous pouvez maintenant utiliser l'identifiant '").append(identifier)
          .append("' pour créer une souscription.");

        return sb.toString();
    }
}