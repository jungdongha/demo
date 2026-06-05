package com.obigo.demodong.domain.portfolio.application.usecase;

import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisResponse;
import com.obigo.demodong.domain.analysis.application.dto.response.StrategyScoreResponse;
import com.obigo.demodong.domain.analysis.application.usecase.AnalysisUseCase;
import com.obigo.demodong.domain.portfolio.application.dto.response.WatchlistScoreResponse;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 관심종목 일괄 점수 조회 유스케이스.
 * 현재가 + 7개 전략 점수를 일괄 조회하여 totalScore 내림차순 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchlistScoreUseCase {

    private final StockReader stockReader;
    private final AnalysisUseCase analysisUseCase;
    private final StockPricePort stockPricePort;

    public List<WatchlistScoreResponse> getScores() {
        List<Stock> watchlist = stockReader.findAllByIsWatchlistTrue();

        if (watchlist.isEmpty()) {
            return List.of();
        }

        List<WatchlistScoreResponse> results = watchlist.stream()
                .map(this::toScoreResponse)
                .toList();

        // totalScore 내림차순 (null은 맨 뒤)
        return results.stream()
                .sorted(Comparator.comparing(
                        WatchlistScoreResponse::totalScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    // ─────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────

    private WatchlistScoreResponse toScoreResponse(Stock stock) {
        BigDecimal currentPrice = fetchCurrentPrice(stock);
        Map<String, Integer> scores = fetchScores(stock.getTicker());
        Integer totalScore = calcTotalScore(scores);

        return new WatchlistScoreResponse(
                stock.getTicker(),
                stock.getName(),
                currentPrice,
                null,           // priceChange: 별도 API 필요 (Phase 9+)
                scores,
                totalScore,
                LocalDateTime.now()
        );
    }

    private BigDecimal fetchCurrentPrice(Stock stock) {
        try {
            return stockPricePort.fetchCurrentPrice(stock);
        } catch (Exception e) {
            log.warn("[WatchlistScore] 현재가 조회 실패 skip — ticker: {}", stock.getTicker());
            return null;
        }
    }

    private Map<String, Integer> fetchScores(String ticker) {
        try {
            AnalysisResponse analysis = analysisUseCase.analyze(ticker);
            Map<String, Integer> scores = new LinkedHashMap<>();
            for (StrategyScoreResponse s : analysis.strategies()) {
                scores.put(s.type(), s.score());
            }
            return Collections.unmodifiableMap(scores);
        } catch (Exception e) {
            log.warn("[WatchlistScore] 분석 조회 실패 skip — ticker: {}", ticker);
            return Map.of();
        }
    }

    private Integer calcTotalScore(Map<String, Integer> scores) {
        if (scores.isEmpty()) return null;
        return (int) scores.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }
}
