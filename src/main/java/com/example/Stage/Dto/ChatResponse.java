package com.example.Stage.Dto;

public record ChatResponse(
        String response,
        String provider,
        String intent,      // ex: "SEARCH_POLICIES"
        Object data         // list of policies found
) {}
