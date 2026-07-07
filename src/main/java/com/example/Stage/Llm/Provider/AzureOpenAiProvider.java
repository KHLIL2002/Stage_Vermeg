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
public class AzureOpenAiProvider implements LlmService {

    @Value("${llm.azure.api-key:none}")
    private String apiKey;

    @Value("${llm.azure.endpoint:none}")
    private String endpoint;

    @Value("${llm.azure.deployment:gpt-4o}")
    private String deployment;

    private final LlmProviderRegistry registry;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public AzureOpenAiProvider(LlmProviderRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register("azure", this);
    }

    @Override
    public String ask(List<LlmMessage> messages, LlmConfig config) {
        if ("none".equals(apiKey)) return "Erreur : clé API Azure non configurée. Demandez les credentials Azure à votre tuteur.";
        try {
            List<Map<String, String>> apiMessages = messages.stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                    .toList();

            Map<String, Object> body = Map.of(
                    "max_tokens", config.maxTokens(),
                    "temperature", config.temperature(),
                    "messages", apiMessages
            );

            String url = endpoint + "/openai/deployments/" + deployment
                    + "/chat/completions?api-version=2024-02-15-preview";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (json.has("error")) return "Erreur Azure : " + json.get("error").get("message").asText();
            return json.get("choices").get(0).get("message").get("content").asText();

        } catch (Exception e) {
            return "Erreur Azure : " + e.getMessage();
        }
    }

    @Override
    public Stream<String> stream(List<LlmMessage> messages, LlmConfig config) {
        return Stream.of(ask(messages, config).split(" "));
    }
}