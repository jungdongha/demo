package com.obigo.demodong.domain.notification.application.scheduler;

import com.obigo.demodong.domain.notification.application.usecase.WatchlistNotifyUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 관심종목 일일 알림 스케줄러.
 * 평일 오전 8:00 자동 발송.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyScheduler {

    private final WatchlistNotifyUseCase watchlistNotifyUseCase;

    @Scheduled(cron = "0 0 8 * * MON-FRI")
    public void sendDailyWatchlistReport() {
        log.info("[Scheduler] 관심종목 일일 알림 발송 시작");
        String result = watchlistNotifyUseCase.execute();
        log.info("[Scheduler] 관심종목 일일 알림 발송 완료 — {}", result);
    }
}
