package com.obigo.demodong.domain.quant.application.scheduler;

import com.obigo.demodong.domain.quant.application.exception.QuantErrorCode;
import com.obigo.demodong.domain.quant.application.usecase.QuantEngineUseCase;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Quant Signal 일배치 스케줄러.
 * KOR: 매 거래일 09:10 (장 개시 10분 후)
 * USA: 매 거래일 22:30 (미국장 개시 후)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuantSignalScheduler {

    private final QuantEngineUseCase quantEngineUseCase;

    @Scheduled(cron = "0 10 9 * * MON-FRI")
    public void runKorBatch() {
        log.info("[QuantScheduler] KOR 배치 시작");
        runBatchSafely(MarketType.KOR);
    }

    @Scheduled(cron = "0 30 22 * * MON-FRI")
    public void runUsaBatch() {
        log.info("[QuantScheduler] USA 배치 시작");
        runBatchSafely(MarketType.USA);
    }

    private void runBatchSafely(MarketType marketType) {
        try {
            quantEngineUseCase.runBatch(marketType);
        } catch (ApplicationException e) {
            if (e.getErrorCode() == QuantErrorCode.MARKET_REGIME_CRISIS) {
                log.warn("[QuantScheduler] {} CRISIS 국면 — 시그널 생성 건너뜀", marketType);
            } else {
                log.error("[QuantScheduler] {} 배치 실패: {}", marketType, e.getMessage());
            }
        } catch (Exception e) {
            log.error("[QuantScheduler] {} 배치 예외: {}", marketType, e.getMessage(), e);
        }
    }
}
