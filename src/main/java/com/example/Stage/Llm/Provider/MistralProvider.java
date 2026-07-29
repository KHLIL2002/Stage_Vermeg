package com.example.Stage.Llm.Provider;

import com.example.Stage.Llm.LlmProviderRegistry;
import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

@Service
public class MistralProvider implements LlmService {

    @Value("${llm.mistral.api-key:none}")
    private String apiKey;

    @Value("${llm.mistral.model:mistral-large-latest}")
    private String model;

    private final LlmProviderRegistry registry;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public MistralProvider(LlmProviderRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register("mistral", this);
    }

    @Override
    public String ask(List<LlmMessage> messages, LlmConfig config) {
        LlmResponse response = askWithTools(messages, List.of(), config);
        return response.text();
    }

    @Override
    public LlmResponse askWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools, LlmConfig config) {
        if ("none".equals(apiKey)) return LlmResponse.text("Erreur : clé API Mistral non configurée.");
        try {
            HttpRequest request = buildHttpRequest(messages, tools, config);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            // Gestion des erreurs API (message d'erreur explicite plutôt qu'une NPE)
            if (json.has("error") || !json.has("choices")) {
                String detail = json.has("error") ? json.get("error").toString() : json.toString();
                return LlmResponse.text("Erreur Mistral : " + detail);
            }

            JsonNode choice = json.get("choices").get(0);
            JsonNode message = choice.get("message");

            // ── Détection des appels d'outils (Tool Calls) ──────────────────────
            JsonNode toolCallsNode = message.get("tool_calls");
            if (toolCallsNode != null && toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                List<LlmToolCall> toolCalls = new ArrayList<>();
                for (JsonNode tc : toolCallsNode) {
                    String id = tc.has("id") ? tc.get("id").asText() : UUID.randomUUID().toString();
                    String name = tc.get("function").get("name").asText();
                    String args = tc.get("function").get("arguments").asText();
                    Map<String, Object> input = (args == null || args.isBlank())
                            ? new HashMap<>()
                            : mapper.readValue(args, Map.class);
                    toolCalls.add(new LlmToolCall(id, name, input));
                }
                return new LlmResponse(null, toolCalls, "tool_calls");
            }

            // ── Réponse textuelle (content peut être null) ──────────────────────
            JsonNode content = message.get("content");
            String text = (content == null || content.isNull()) ? "" : content.asText();
            return LlmResponse.text(text);

        } catch (Exception e) {
            return LlmResponse.text("Erreur technique Mistral : " + e.getMessage());
        }
    }

    @Override
    public Stream<String> stream(List<LlmMessage> messages, LlmConfig config) {
        // Version simplifiée pour le streaming
        return Stream.of(ask(messages, config).split(" "));
    }

    private HttpRequest buildHttpRequest(List<LlmMessage> messages, List<LlmToolDefinition> tools, LlmConfig config) throws Exception {
        // Mistral EXIGE le champ 'name' sur les messages de rôle 'tool'
        // (contrairement à Groq/OpenAI). On reconstruit la correspondance
        // tool_call_id -> nom de fonction depuis les messages 'assistant'.
        Map<String, String> toolCallNames = new HashMap<>();
        for (LlmMessage m : messages) {
            if (m.toolCalls() != null) {
                for (LlmToolCall tc : m.toolCalls()) {
                    toolCallNames.put(tc.id(), tc.name());
                }
            }
        }

        List<Map<String, Object>> apiMessages = new ArrayList<>();
        for (LlmMessage m : messages) {
            Map<String, Object> msg = new HashMap<>();

            boolean isToolResult = "tool_result".equals(m.role()) || "tool".equals(m.role());
            boolean isAssistantToolCall = "assistant".equals(m.role())
                    && m.toolCalls() != null && !m.toolCalls().isEmpty();

            if (isToolResult) {
                // Résultat d'exécution d'un outil
                msg.put("role", "tool");
                msg.put("tool_call_id", m.toolCallId());
                String name = toolCallNames.get(m.toolCallId());
                if (name != null) msg.put("name", name); // requis par Mistral
                msg.put("content", m.content() != null ? m.content() : "");

            } else if (isAssistantToolCall) {
                // L'assistant demande l'appel d'un ou plusieurs outils
                msg.put("role", "assistant");
                msg.put("content", m.content() != null ? m.content() : "");

                List<Map<String, Object>> tCalls = new ArrayList<>();
                for (LlmToolCall tc : m.toolCalls()) {
                    tCalls.add(Map.of(
                            "id", tc.id(),
                            "type", "function",
                            "function", Map.of(
                                    "name", tc.name(),
                                    "arguments", mapper.writeValueAsString(tc.input()))
                    ));
                }
                msg.put("tool_calls", tCalls);

            } else {
                // Message standard (system / user / assistant textuel)
                msg.put("role", m.role());
                msg.put("content", m.content() != null ? m.content() : "");
            }
            apiMessages.add(msg);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", apiMessages);
        body.put("temperature", config.temperature());

        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolDefs = new ArrayList<>();
            for (LlmToolDefinition t : tools) {
                toolDefs.add(Map.of(
                        "type", "function",
                        "function", Map.of("name", t.name(), "description", t.description(), "parameters", t.parameters())
                ));
            }
            body.put("tools", toolDefs);
            body.put("tool_choice", "auto");
        }

        return HttpRequest.newBuilder()
                .uri(URI.create("https://api.mistral.ai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
    }
}
