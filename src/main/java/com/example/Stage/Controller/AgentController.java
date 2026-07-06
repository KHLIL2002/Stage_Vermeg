package com.example.Stage.Controller;

import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.LlmConfig;
import com.example.Stage.Llm.Model.LlmMessage;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class AgentController {

    private final LlmService llmService;

    public AgentController(LlmService llmService) {
        this.llmService = llmService;
    }

    @PostMapping("/agent")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        List<LlmMessage> messages = List.of(
                LlmMessage.system("Tu es un agent spécialisé en contrats d'assurance."),
                LlmMessage.user(userMessage)
        );

        String response = llmService.ask(messages, LlmConfig.defaults());

        return Map.of("response", response);
    }
}