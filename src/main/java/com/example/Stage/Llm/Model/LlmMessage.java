package com.example.Stage.Llm.Model;

import java.util.List;

public record LlmMessage(
        String role,
        String content,
        List<LlmToolCall> toolCalls,
        String toolCallId
) {
    // Méthodes statiques pratiques
    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null);
    }

    public static LlmMessage toolResult(String toolCallId, String content) {
        return new LlmMessage("tool_result", content, null, toolCallId);
    }
}