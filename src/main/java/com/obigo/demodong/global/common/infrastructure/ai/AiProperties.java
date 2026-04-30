package com.obigo.demodong.global.common.infrastructure.ai;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.ai.openai")
public record AiProperties (
        String apiKey,
        String baseUrl,
        String model,
        int maxTokens
){}
