package com.example.Stage.Llm.Provider;

import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.LlmConfig;
import com.example.Stage.Llm.Model.LlmMessage;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "claude")
public class ClaudeProvider implements LlmService {

    @Value("${llm.api-key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String ask(List<LlmMessage> messages, LlmConfig config) {
        try {
            String systemPrompt = messages.stream()
                    .filter(m -> "system".equals(m.role()))
                    .map(LlmMessage::content)
                    .findFirst()
                    .orElse("");

            List<Map<String, String>> apiMessages = messages.stream()
                    .filter(m -> !"system".equals(m.role()))
                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                    .toList();

            Map<String, Object> body = Map.of(
                    "model", "claude-sonnet-4-6",
                    "max_tokens", config.maxTokens(),
                    "system", systemPrompt,
                    "messages", apiMessages
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String rawBody = response.body();

            // Debug — affiche la réponse brute
            System.out.println("=== Claude raw response ===");
            System.out.println(rawBody);
            System.out.println("===========================");

            JsonNode json = mapper.readTree(rawBody);

            if (json.has("error")) {
                return "Erreur Claude : " + json.get("error").get("message").asText();
            }

            return json.get("content").get(0).get("text").asText();

        } catch (Exception e) {
            return "Erreur Claude : " + e.getMessage();
        }
    }

    @Override
    public Stream<String> stream(List<LlmMessage> messages, LlmConfig config) {
        return Stream.of(ask(messages, config).split(" "));
    }
}