package com.example.Stage.Llm.Provider;

import com.example.Stage.Llm.LlmProviderRegistry;
import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

@Service
public class GroqProvider implements LlmService {

    @Value("${llm.groq.api-key:none}")
    private String apiKey;
    @Value("${llm.groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final LlmProviderRegistry registry;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqProvider(LlmProviderRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register("groq", this);
    }

    @Override
    public String ask(List<LlmMessage> messages, LlmConfig config) {
        LlmResponse response = askWithTools(messages, List.of(), config);
        return response.text();
    }

    @Override
    public LlmResponse askWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools, LlmConfig config) {
        if ("none".equals(apiKey)) return LlmResponse.text("Erreur : clé API Groq non configurée.");

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = buildHttpRequest(messages, tools, config, false);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = mapper.readTree(response.body());

                if (json.has("error")) {
                    String errMsg = json.get("error").path("message").asText("");
                    boolean rateLimited = response.statusCode() == 429
                            || errMsg.toLowerCase().contains("rate limit");

                    // Rate limit (tokens/minute) : on attend le délai suggéré puis on réessaie.
                    if (rateLimited && attempt < maxAttempts) {
                        Thread.sleep(parseRetryDelayMs(errMsg));
                        continue;
                    }
                    if (rateLimited) {
                        return LlmResponse.text("Le service IA est momentanément saturé (limite de tokens par minute atteinte). "
                                + "Merci de patienter quelques secondes puis de réessayer.");
                    }
                    return LlmResponse.text("Erreur Groq : " + errMsg);
                }

                JsonNode choice = json.get("choices").get(0);
                JsonNode message = choice.get("message");

                if (message.has("tool_calls")) {
                    List<LlmToolCall> toolCalls = new ArrayList<>();
                    for (JsonNode tc : message.get("tool_calls")) {
                        String id = tc.get("id").asText();
                        String name = tc.get("function").get("name").asText();
                        String args = tc.get("function").get("arguments").asText();
                        Map<String, Object> input = mapper.readValue(args, Map.class);
                        toolCalls.add(new LlmToolCall(id, name, input));
                    }
                    return new LlmResponse(null, toolCalls, "tool_calls");
                }

                return LlmResponse.text(message.get("content").asText());

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return LlmResponse.text("Erreur Groq : requête interrompue.");
            } catch (Exception e) {
                return LlmResponse.text("Erreur Groq : " + e.getMessage());
            }
        }
        return LlmResponse.text("Le service IA est momentanément saturé. Merci de réessayer dans un instant.");
    }

    /**
     * Extrait le délai d'attente conseillé par Groq (ex: "try again in 12.93s").
     * Renvoie une valeur en millisecondes, plafonnée à 15s pour ne pas bloquer trop longtemps.
     */
    private long parseRetryDelayMs(String message) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("try again in ([0-9.]+)s").matcher(message);
        if (m.find()) {
            try {
                double seconds = Double.parseDouble(m.group(1));
                long ms = (long) Math.ceil(seconds * 1000) + 250;
                return Math.min(ms, 15000);
            } catch (NumberFormatException ignored) { }
        }
        return 2000;
    }

    @Override
    public Stream<String> stream(List<LlmMessage> messages, LlmConfig config) {
        // Pour l'instant, on utilise la version simple pour corriger la compilation
        // On appelle ask() et on découpe le résultat
        return Stream.of(ask(messages, config).split(" "));
    }

    private HttpRequest buildHttpRequest(List<LlmMessage> messages, List<LlmToolDefinition> tools, LlmConfig config, boolean isStream) throws Exception {
        List<Map<String, Object>> apiMessages = new ArrayList<>();

        for (LlmMessage m : messages) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", m.role());

            // Si c'est un résultat d'outil (role tool ou tool_result)
            if ("tool_result".equals(m.role()) || "tool".equals(m.role())) {
                msg.put("role", "tool"); // Groq attend "tool"
                msg.put("tool_call_id", m.toolCallId());
                msg.put("content", m.content());
            }
            // Si c'est l'assistant qui appelle un outil
            else if ("assistant".equals(m.role()) && m.toolCalls() != null && !m.toolCalls().isEmpty()) {
                msg.put("content", m.content()); // souvent null ou vide

                List<Map<String, Object>> toolCallsJson = new ArrayList<>();
                for (LlmToolCall tc : m.toolCalls()) {
                    Map<String, Object> toolCall = new HashMap<>();
                    toolCall.put("id", tc.id());
                    toolCall.put("type", "function"); // <-- C'ÉTAIT ÇA LE PROBLÈME

                    Map<String, Object> function = new HashMap<>();
                    function.put("name", tc.name());
                    // Transformer la Map d'arguments en String JSON
                    function.put("arguments", mapper.writeValueAsString(tc.input()));

                    toolCall.put("function", function);
                    toolCallsJson.add(toolCall);
                }
                msg.put("tool_calls", toolCallsJson);
            }
            // Message standard (user ou system)
            else {
                msg.put("content", m.content());
            }
            apiMessages.add(msg);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", apiMessages);
        body.put("stream", isStream);
        body.put("temperature", config.temperature());

        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolDefs = tools.stream().map(t -> Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", t.name(),
                            "description", t.description(),
                            "parameters", t.parameters()
                    )
            )).toList();
            body.put("tools", toolDefs);
        }

        return HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
    }
}