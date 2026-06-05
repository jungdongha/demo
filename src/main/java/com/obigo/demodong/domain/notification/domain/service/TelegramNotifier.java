package com.obigo.demodong.domain.notification.domain.service;

import com.obigo.demodong.domain.notification.infrastructure.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * 텔레그램 봇 메시지 발송 서비스.
 *
 * <p>TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID 미설정 시 no-op 처리 (서버 기동 차단 없음).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotifier {

    private final TelegramProperties telegramProperties;

    /**
     * 텔레그램 채널로 메시지를 발송한다.
     *
     * @param message 발송할 메시지 텍스트
     * @throws RuntimeException 발송 실패 시 (호출자가 처리)
     */
    public void send(String message) {
        if (!telegramProperties.isConfigured()) {
            log.warn("[Telegram] 봇 토큰 또는 채팅 ID 미설정 — 발송 skip");
            return;
        }

        String url = "https://api.telegram.org/bot" + telegramProperties.getBotToken() + "/sendMessage";

        WebClient.create()
                .post()
                .uri(url)
                .bodyValue(Map.of(
                        "chat_id", telegramProperties.getChatId(),
                        "text", message,
                        "parse_mode", "HTML"
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

        log.info("[Telegram] 메시지 발송 성공 — {} chars", message.length());
    }
}
