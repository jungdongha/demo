package com.obigo.demodong.global.common.infrastructure.telegram;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfig {
    @Bean("telegramWebClient")
    public WebClient telegramWebClient(TelegramProperties telegramProperties) {
        return WebClient.builder()
                .baseUrl("https://api.telegram.org/bot" + telegramProperties.botToken())
                .build();
    }
}
