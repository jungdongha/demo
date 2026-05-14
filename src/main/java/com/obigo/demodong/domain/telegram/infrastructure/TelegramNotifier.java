package com.obigo.demodong.domain.telegram.infrastructure;


import com.obigo.demodong.global.common.infrastructure.telegram.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class TelegramNotifier {
    private final WebClient telegramWebClient;
    private final TelegramProperties telegramProperties;

    public TelegramNotifier(@Qualifier("telegramWebClient") WebClient telegramWebClient,
                            TelegramProperties telegramProperties) {
        this.telegramWebClient = telegramWebClient;
        this.telegramProperties = telegramProperties;
    }

    public void sendMessage(String text) {
        telegramWebClient.post()
                .uri("/sendMessage")
                .bodyValue(Map.of(
                        "chat_id", telegramProperties.chatId(),
                        "text", text,
                        "parse_mode", "HTML"
                ))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        result -> log.info("텔레그램 메시지 전송 완료"),
                        error -> log.error("텔레그램 전송 실패: {}", error.getMessage())
                );
    }
}
