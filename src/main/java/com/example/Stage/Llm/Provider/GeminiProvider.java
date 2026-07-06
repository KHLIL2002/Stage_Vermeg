package com.example.Stage.Llm.Provider;

import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.LlmConfig;
import com.example.Stage.Llm.Model.LlmMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "llm.provider", havingValue = "gemini")
public class GeminiProvider implements LlmService {

    @Value("${llm.api-key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String ask(List<LlmMessage> messages, LlmConfig config) {
        try {
            List<Map<String, String>> apiMessages = messages.stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                    .toList();

            Map<String, Object> body = Map.of(
                    "model", "gemini-2.5-flash",
                    "messages", apiMessages
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Debug
            System.out.println("=== Gemini status: " + response.statusCode() + " ===");
            System.out.println(response.body());
            System.out.println("===============");

            JsonNode json = mapper.readTree(response.body());

            if (json.has("error")) {
                return "Erreur Gemini : " + json.get("error").get("message").asText();
            }

            return json.get("choices").get(0).get("message").get("content").asText();

        } catch (Exception e) {
            return "Erreur Gemini : " + e.getMessage();
        }
    }

    @Override
    public Stream<String> stream(List<LlmMessage> messages, LlmConfig config) {
        return Stream.of(ask(messages, config).split(" "));
    }
}