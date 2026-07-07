package com.example.Stage.Llm;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LlmProviderRegistry {

    private final Map<String, LlmService> providers = new HashMap<>();

    public void register(String name, LlmService provider) {
        providers.put(name, provider);
    }

    public LlmService get(String name) {
        LlmService provider = providers.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("Provider inconnu : " + name
                    + ". Disponibles : " + providers.keySet());
        }
        return provider;
    }

    public Map<String, LlmService> getAll() {
        return providers;
    }
}