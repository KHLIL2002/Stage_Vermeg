package com.example.Stage.Llm.Provider;

import com.example.Stage.Llm.LlmService;
import com.example.Stage.Llm.Model.LlmConfig;
import com.example.Stage.Llm.Model.LlmMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockProvider implements LlmService {

    @Override
    public String ask(List<LlmMessage> messages, LlmConfig config) {
        String userMessage = messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(LlmMessage::content)
                .orElse("");

        return mockResponse(userMessage);
    }

    @Override
    public Stream<String> stream(List<LlmMessage> messages, LlmConfig config) {
        return Stream.of(ask(messages, config).split(" "));
    }

    private String mockResponse(String input) {
        String lower = input.toLowerCase();

        if (lower.contains("contrat") && lower.contains("vigueur"))
            return "Contrats en vigueur :\n"
                    + "• CTR-2026-00001 — Dupont Marie — Auto — 720 €\n"
                    + "• CTR-2026-00002 — Dupont Marie — Habitation — 480 €\n"
                    + "• CTR-2026-00005 — SCI Les Oliviers — MRP — 3 500 €";

        if (lower.contains("dupont"))
            return "Dupont Marie — PREMIUM — 2 contrats en vigueur — Prime totale : 1 200 €/an";

        if (lower.contains("vip"))
            return "Clients VIP : Durand Pierre, SCI Les Oliviers, Cabinet Moreau & Fils";

        if (lower.contains("bonjour") || lower.contains("salut"))
            return "Bonjour ! Je suis l'agent IA Contrats. Posez-moi une question sur vos contrats.";

        return "Agent IA Contrats (mock). Essayez : \"contrats en vigueur\", \"client Dupont\", \"clients VIP\"";
    }
}