package com.example.Stage.Llm;

import com.example.Stage.Llm.Model.LlmConfig;
import com.example.Stage.Llm.Model.LlmMessage;
import com.example.Stage.Llm.Model.LlmResponse;
import com.example.Stage.Llm.Model.LlmToolDefinition;

import java.util.List;
import java.util.stream.Stream;

public interface LlmService {

    String ask(List<LlmMessage> messages, LlmConfig config);

    Stream<String> stream(List<LlmMessage> messages, LlmConfig config);

    // Nouvelle méthode avec function calling
    default LlmResponse askWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools, LlmConfig config) {
        // Par défaut, pas de support tools — juste du texte
        return LlmResponse.text(ask(messages, config));
    }


}