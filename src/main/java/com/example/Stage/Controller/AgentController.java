package com.example.Stage.Controller;

import com.example.Stage.Llm.LlmProviderRegistry;
import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.LlmConfig;
import com.example.Stage.Llm.Model.LlmMessage;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class AgentController {

    private final LlmProviderRegistry registry;

    public AgentController(LlmProviderRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/agent")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String provider = request.getOrDefault("provider", "groq");

        LlmService llmService = registry.get(provider);

        List<LlmMessage> messages = List.of(
                LlmMessage.system("Tu es un agent spécialisé en contrats d'assurance."),
                LlmMessage.user(userMessage)
        );

        String response = llmService.ask(messages, LlmConfig.defaults());
        return Map.of("response", response, "provider", provider);
    }

    @GetMapping("/providers")
    public Set<String> listProviders() {
        return registry.getAll().keySet();
    }
}