package com.example.Stage.Llm.Model;

public record LlmConfig(
        String model,
        double temperature,
        int maxTokens
) {
    public static LlmConfig defaults() {
        return new LlmConfig("default", 0.3, 1024);
    }
}
