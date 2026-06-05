package com.obigo.demodong.domain.notification.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 텔레그램 봇 설정 프로퍼티.
 * TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID 환경 변수로 주입.
 * 미설정 시 빈 문자열 → TelegramNotifier에서 no-op 처리.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private String botToken = "";
    private String chatId = "";

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank()
                && chatId != null && !chatId.isBlank();
    }
}
