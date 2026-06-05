package com.obigo.demodong.domain.analysis.application.scheduler;

import com.obigo.demodong.domain.analysis.domain.repository.AnalysisHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 분석 히스토리 자동 정리 스케줄러.
 * 매일 새벽 2시 — 90일 초과 데이터 삭제.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryCleanupScheduler {

    private final AnalysisHistoryRepository analysisHistoryRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanup() {
        LocalDate cutoff = LocalDate.now().minusDays(90);
        log.info("[Scheduler] 분석 히스토리 정리 시작 — cutoff: {}", cutoff);
        analysisHistoryRepository.deleteByAnalyzeDateBefore(cutoff);
        log.info("[Scheduler] 분석 히스토리 정리 완료");
    }
}
