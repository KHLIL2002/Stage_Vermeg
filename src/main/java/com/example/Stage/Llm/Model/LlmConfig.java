package com.example.Stage.Llm.Model;

public record LlmConfig(
        String model,
        double temperature,
        int maxTokens
) {
}
