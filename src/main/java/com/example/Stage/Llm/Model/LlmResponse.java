package com.example.Stage.Llm.Model;

import java.util.List;

public record LlmResponse(
        String text,
        List<LlmToolCall> toolCalls,
        String stopReason
) {
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public static LlmResponse text(String content) {
        return new LlmResponse(content, List.of(), "stop");
    }
}