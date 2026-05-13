package com.canonical.copyrightagent.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelService {
    @Autowired
    private OpenAiChatModel baseChatModel;

    public ChatModel createModel(String model) {
        return baseChatModel.mutate().defaultOptions(OpenAiChatOptions.builder()
                        .model(model).build())
                .build();
    }

}
