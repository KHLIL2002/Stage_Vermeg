package com.example.Stage.Llm.Model;

import java.util.Map;

public record LlmToolCall(
        String id,
        String name,
        Map<String, Object> input
) {}