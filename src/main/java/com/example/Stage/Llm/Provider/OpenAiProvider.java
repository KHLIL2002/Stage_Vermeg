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
public class OpenAiProvider implements LlmService {

    @Value("${llm.openai.api-key:none}")
    private String apiKey;

    private final LlmProviderRegistry registry;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiProvider(LlmProviderRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register("openai", this);
    }

    @Override
    public String ask(List<LlmMessage> messages, LlmConfig config) {
        if ("none".equals(apiKey)) return "Erreur : clé API OpenAI non configurée.";
        try {
            List<Map<String, String>> apiMessages = messages.stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                    .toList();

            Map<String, Object> body = Map.of(
                    "model", "gpt-4o",
                    "max_tokens", config.maxTokens(),
                    "temperature", config.temperature(),
                    "messages", apiMessages
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (json.has("error")) return "Erreur OpenAI : " + json.get("error").get("message").asText();
            return json.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            return "Erreur OpenAI : " + e.getMessage();
        }
    }

    @Override
    public Stream<String> stream(List<LlmMessage> messages, LlmConfig config) {
        return Stream.of(ask(messages, config).split(" "));
    }
}