package com.obigo.demodong.domain.notification.presentation;

import com.obigo.demodong.domain.notification.application.usecase.WatchlistNotifyUseCase;
import com.obigo.demodong.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 관리 REST API 컨트롤러.
 *
 * <p>POST /api/admin/notify/watchlist — 관심종목 분석 결과 즉시 텔레그램 발송</p>
 */
@Tag(name = "알림 관리", description = "텔레그램 알림 수동 발송")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notify")
public class NotifyController {

    private final WatchlistNotifyUseCase watchlistNotifyUseCase;

    @Operation(
            summary = "관심종목 텔레그램 알림 즉시 발송",
            description = "관심종목 전체를 분석하여 텔레그램 채널로 발송한다. TELEGRAM_BOT_TOKEN 미설정 시 skip."
    )
    @PostMapping("/watchlist")
    public ApiResponse<String> sendWatchlistReport() {
        log.info("[API] POST /api/admin/notify/watchlist");
        String result = watchlistNotifyUseCase.execute();
        return ApiResponse.ok(NotifyResponseCode.NOTIFY_SUCCESS, result);
    }
}
