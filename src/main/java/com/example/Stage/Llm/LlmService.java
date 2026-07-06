package com.example.Stage.Llm;

import com.example.Stage.Llm.Model.LlmConfig;
import com.example.Stage.Llm.Model.LlmMessage;

import java.util.List;
import java.util.stream.Stream;

public interface LlmService {

    String ask(List<LlmMessage> messages, LlmConfig config);

    Stream<String> stream(List<LlmMessage> messages, LlmConfig config);

}