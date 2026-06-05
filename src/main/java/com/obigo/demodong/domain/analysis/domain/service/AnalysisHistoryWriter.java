package com.obigo.demodong.domain.analysis.domain.service;

import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisResponse;
import com.obigo.demodong.domain.analysis.application.dto.response.StrategyScoreResponse;
import com.obigo.demodong.domain.analysis.domain.entity.AnalysisHistory;
import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.repository.AnalysisHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 분석 히스토리 저장 도메인 서비스.
 *
 * <p>{@code @Async}로 비동기 처리 — 저장 실패가 API 응답을 차단하지 않음.</p>
 * <p>당일 ticker 레코드가 있으면 UPDATE(upsert), 없으면 INSERT.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisHistoryWriter {

    private final AnalysisHistoryRepository analysisHistoryRepository;

    @Async
    @Transactional
    public void upsert(AnalysisResponse analysis) {
        try {
            String ticker = analysis.ticker();
            LocalDate today = LocalDate.now();

            Map<String, Integer> scoreMap = analysis.strategies().stream()
                    .collect(Collectors.toMap(StrategyScoreResponse::type, StrategyScoreResponse::score));

            int total = (int) scoreMap.values().stream().mapToInt(Integer::intValue).average().orElse(0);

            analysisHistoryRepository.findByTickerAndAnalyzeDate(ticker, today)
                    .ifPresentOrElse(
                            existing -> existing.updateScores(
                                    total,
                                    scoreMap.get(StrategyType.CANSLIM.name()),
                                    scoreMap.get(StrategyType.DUAL_MOMENTUM.name()),
                                    scoreMap.get(StrategyType.SEASONALITY.name()),
                                    scoreMap.get(StrategyType.MINERVINI.name()),
                                    scoreMap.get(StrategyType.MAGIC_FORMULA.name()),
                                    scoreMap.get(StrategyType.PIOTROSKI.name()),
                                    scoreMap.get(StrategyType.MEAN_REVERSION.name())
                            ),
                            () -> analysisHistoryRepository.save(
                                    AnalysisHistory.builder()
                                            .ticker(ticker)
                                            .analyzeDate(today)
                                            .totalScore(total)
                                            .canslimScore(scoreMap.get(StrategyType.CANSLIM.name()))
                                            .momentumScore(scoreMap.get(StrategyType.DUAL_MOMENTUM.name()))
                                            .seasonalityScore(scoreMap.get(StrategyType.SEASONALITY.name()))
                                            .minerviniScore(scoreMap.get(StrategyType.MINERVINI.name()))
                                            .magicFormulaScore(scoreMap.get(StrategyType.MAGIC_FORMULA.name()))
                                            .piotroskiScore(scoreMap.get(StrategyType.PIOTROSKI.name()))
                                            .meanReversionScore(scoreMap.get(StrategyType.MEAN_REVERSION.name()))
                                            .build()
                            )
                    );

            log.debug("[AnalysisHistory] upsert 완료 — ticker: {}, date: {}", ticker, today);
        } catch (Exception e) {
            log.warn("[AnalysisHistory] 저장 실패 (무시) — ticker: {}, reason: {}", analysis.ticker(), e.getMessage());
        }
    }
}
