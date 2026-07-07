package com.example.Stage.Llm.Provider;

import com.example.Stage.Llm.LlmProviderRegistry;
import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.LlmConfig;
import com.example.Stage.Llm.Model.LlmMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class GeminiProvider implements LlmService {

    @Value("${llm.gemini.api-key:none}")
    private String apiKey;

    private final LlmProviderRegistry registry;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiProvider(LlmProviderRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register("gemini", this);
    }

    @Override
    public String ask(List<LlmMessage> messages, LlmConfig config) {
        if ("none".equals(apiKey)) return "Erreur : clé API Gemini non configurée.";
        try {
            // Convertir les messages au format Gemini natif
            List<Map<String, Object>> contents = messages.stream()
                    .filter(m -> !"system".equals(m.role()))
                    .map(m -> Map.of(
                            "role", "user".equals(m.role()) ? "user" : "model",
                            "parts", List.of(Map.of("text", m.content()))
                    ))
                    .toList();

            // System prompt dans systemInstruction
            String systemPrompt = messages.stream()
                    .filter(m -> "system".equals(m.role()))
                    .map(LlmMessage::content)
                    .findFirst().orElse("");

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("contents", contents);
            if (!systemPrompt.isEmpty()) {
                body.put("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ));
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("=== Gemini status: " + response.statusCode() + " ===");

            JsonNode json = mapper.readTree(response.body());

            if (json.has("error")) {
                return "Erreur Gemini : " + json.get("error").get("message").asText();
            }

            return json.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();

        } catch (Exception e) {
            return "Erreur Gemini : " + e.getMessage();
        }
    }

    @Override
    public Stream<String> stream(List<LlmMessage> messages, LlmConfig config) {
        return Stream.of(ask(messages, config).split(" "));
    }
}