package com.obigo.demodong.global.common.infrastructure.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "telegram")

public record TelegramProperties (
        String botToken,
        String chatId
){
}
