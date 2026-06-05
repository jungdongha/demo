package com.obigo.demodong.domain.portfolio.application.usecase;

import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisResponse;
import com.obigo.demodong.domain.analysis.application.dto.response.StrategyScoreResponse;
import com.obigo.demodong.domain.analysis.application.usecase.AnalysisUseCase;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioItemDetail;
import com.obigo.demodong.domain.portfolio.application.dto.response.PortfolioSummaryResponse;
import com.obigo.demodong.domain.portfolio.domain.entity.PortfolioDetail;
import com.obigo.demodong.domain.portfolio.domain.repository.PortfolioDetailRepository;
import com.obigo.demodong.domain.portfolio.domain.service.PortfolioSummaryService;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 포트폴리오 요약 유스케이스.
 * 보유 종목별 현재가 + 전략 점수를 조회하여 수익률·점수를 집계한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioSummaryUseCase {

    private final PortfolioDetailRepository portfolioDetailRepository;
    private final StockPricePort stockPricePort;
    private final AnalysisUseCase analysisUseCase;
    private final PortfolioSummaryService portfolioSummaryService;

    public PortfolioSummaryResponse getSummary() {
        List<PortfolioDetail> details = portfolioDetailRepository.findAllByDeletedFalse();

        if (details.isEmpty()) {
            return new PortfolioSummaryResponse(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, List.of()
            );
        }

        List<PortfolioItemDetail> items = details.stream()
                .map(this::toItemDetail)
                .toList();

        return portfolioSummaryService.calculate(items);
    }

    // ─────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────

    private PortfolioItemDetail toItemDetail(PortfolioDetail detail) {
        String ticker = detail.getStock().getTicker();

        BigDecimal currentPrice = fetchCurrentPrice(detail);
        BigDecimal profitRate = calcProfitRate(detail.getAvgPrice(), currentPrice);
        Map<String, Integer> scores = fetchScores(ticker);

        return new PortfolioItemDetail(
                ticker,
                detail.getStock().getName(),
                detail.getQuantity(),
                detail.getAvgPrice(),
                currentPrice,
                profitRate,
                scores
        );
    }

    private BigDecimal fetchCurrentPrice(PortfolioDetail detail) {
        try {
            return stockPricePort.fetchCurrentPrice(detail.getStock());
        } catch (Exception e) {
            log.warn("[Summary] 현재가 조회 실패 skip — ticker: {}", detail.getStock().getTicker());
            return null;
        }
    }

    private BigDecimal calcProfitRate(BigDecimal avgPrice, BigDecimal currentPrice) {
        if (currentPrice == null || avgPrice.compareTo(BigDecimal.ZERO) == 0) return null;
        return currentPrice.subtract(avgPrice)
                .divide(avgPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Integer> fetchScores(String ticker) {
        try {
            AnalysisResponse analysis = analysisUseCase.analyze(ticker);
            Map<String, Integer> scores = new LinkedHashMap<>();
            for (StrategyScoreResponse s : analysis.strategies()) {
                scores.put(s.type(), s.score());
            }
            return scores;
        } catch (Exception e) {
            log.warn("[Summary] 분석 조회 실패 skip — ticker: {}", ticker);
            return Map.of();
        }
    }
}
