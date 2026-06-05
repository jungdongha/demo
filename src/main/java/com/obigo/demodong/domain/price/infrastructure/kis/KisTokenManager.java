package com.obigo.demodong.domain.price.infrastructure.kis;

import com.obigo.demodong.global.common.infrastructure.kis.KisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisTokenManager {

    private final WebClient kisWebClient;
    private final KisProperties kisProperties;

    private final AtomicReference<String> accessToken = new AtomicReference<>();

    // 23시간마다 자동 갱신 (KIS 토큰 유효시간 24h)
    // 최초 토큰은 getAccessToken() 최초 호출 시 lazy 발급 — 앱 재시작 시 rate limit(1회/분) 방지
    @Scheduled(fixedDelay = 23 * 60 * 60 * 1000L, initialDelay = 23 * 60 * 60 * 1000L)
    public void refreshToken() {
        try {
            Map<String, String> body = Map.of(
                    "grant_type", "client_credentials",
                    "appkey", kisProperties.appKey(),
                    "appsecret", kisProperties.appSecret()
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = kisWebClient.post()
                    .uri("/oauth2/tokenP")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (response != null && response.containsKey("access_token")) {
                accessToken.set((String) response.get("access_token"));
                log.info("[KIS] 토큰 갱신 완료");
            } else {
                log.warn("[KIS] 토큰 응답에 access_token 없음");
            }
        } catch (Exception e) {
            log.error("[KIS] 토큰 발급 실패: {}", e.getMessage());
        }
    }

    public String getAccessToken() {
        if (accessToken.get() == null) {
            refreshToken();
        }
        return accessToken.get();
    }

    /** 실전/모의 환경에 따라 tr_id 반환 */
    public String trId(String realTrId, String vtsTrId) {
        return kisProperties.baseUrl().contains("vts") ? vtsTrId : realTrId;
    }
}
