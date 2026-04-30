package com.obigo.demodong.global.common.infrastructure.ai;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {
    //groq (openai-compatible)
    @Bean("groqChatClient")
    ChatClient groqChatClient(AiProperties aiProperties) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(aiProperties.baseUrl())
                .apiKey(aiProperties.apiKey())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiProperties.model())
                .maxTokens(aiProperties.maxTokens())
                .build();
        return ChatClient.builder(OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(options)
                        .build())
                .build();
    }
}
