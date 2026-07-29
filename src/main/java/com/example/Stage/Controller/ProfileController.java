package com.example.Stage.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
public class ProfileController {

    // ── Config Keycloak (surchargeables dans application.properties) ──────────
    @Value("${keycloak.base-url:http://localhost:8180}")
    private String baseUrl;
    @Value("${keycloak.realm:insurance}")
    private String realm;
    @Value("${keycloak.frontend-client-id:insurance-frontend}")
    private String frontendClientId;
    @Value("${keycloak.admin.realm:master}")
    private String adminRealm;
    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;
    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;
    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest body) {

        // 1. Utilisateur authentifié (depuis le JWT)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt)) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié."));
        }
        String username = (String) jwt.getTokenAttributes().get("preferred_username");
        String userId   = (String) jwt.getTokenAttributes().get("sub");

        String current = body.currentPassword();
        String next    = body.newPassword();

        // 2. Validations basiques
        if (current == null || current.isBlank() || next == null || next.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Tous les champs sont obligatoires."));
        if (next.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Le nouveau mot de passe doit contenir au moins 6 caractères."));
        if (userId == null || username == null)
            return ResponseEntity.status(401).body(Map.of("error", "Identité utilisateur introuvable dans le token."));

        // 3. Vérifier l'ancien mot de passe (grant password sur le realm applicatif)
        try {
            HttpResponse<String> verify = post(
                    baseUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                    form(Map.of(
                            "grant_type", "password",
                            "client_id", frontendClientId,
                            "username", username,
                            "password", current
                    )));
            if (verify.statusCode() != 200) {
                return ResponseEntity.status(400).body(Map.of("error", "Le mot de passe actuel est incorrect."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Impossible de vérifier le mot de passe actuel : " + e.getMessage()));
        }

        // 4. Obtenir un token admin (realm master)
        String adminToken;
        try {
            HttpResponse<String> adminResp = post(
                    baseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token",
                    form(Map.of(
                            "grant_type", "password",
                            "client_id", adminClientId,
                            "username", adminUsername,
                            "password", adminPassword
                    )));
            if (adminResp.statusCode() != 200) {
                return ResponseEntity.status(502).body(Map.of("error",
                        "Échec de l'authentification admin Keycloak (HTTP " + adminResp.statusCode()
                        + "). Réponse : " + adminResp.body()
                        + " — Vérifiez keycloak.admin.username / keycloak.admin.password."));
            }
            JsonNode json = mapper.readTree(adminResp.body());
            adminToken = json.get("access_token").asText();
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Erreur lors de l'obtention du token admin : " + e.getMessage()));
        }

        // 5. Réinitialiser le mot de passe via l'Admin API
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "password");
            payload.put("value", next);
            payload.put("temporary", false);

            HttpResponse<String> reset = http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + adminToken)
                            .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            if (reset.statusCode() == 204 || reset.statusCode() == 200) {
                return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès."));
            }
            return ResponseEntity.status(502).body(Map.of("error",
                    "Échec de la mise à jour du mot de passe (Keycloak a répondu " + reset.statusCode() + ")."));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Erreur lors de la mise à jour : " + e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HttpResponse<String> post(String url, String body) throws Exception {
        return http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String form(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
