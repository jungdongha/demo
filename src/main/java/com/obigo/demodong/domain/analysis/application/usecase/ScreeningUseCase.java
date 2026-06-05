package com.obigo.demodong.domain.analysis.application.usecase;

import com.obigo.demodong.domain.analysis.application.dto.request.ScreeningRequest;
import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisResponse;
import com.obigo.demodong.domain.analysis.application.dto.response.ScreeningResult;
import com.obigo.demodong.domain.analysis.domain.service.ScreeningService;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 종목 스크리닝 유스케이스.
 *
 * <p>처리 순서:</p>
 * <ol>
 *   <li>tickers 미지정 시 관심종목 전체 로드</li>
 *   <li>각 ticker 분석 (AnalysisUseCase — @Cacheable 경유)</li>
 *   <li>ScreeningService로 조건 필터링 + 정렬</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreeningUseCase {

    private final AnalysisUseCase analysisUseCase;
    private final ScreeningService screeningService;
    private final StockReader stockReader;

    public List<ScreeningResult> screen(ScreeningRequest request) {
        List<String> tickers = resolveTickers(request);
        if (tickers.isEmpty()) {
            log.info("[Screening] 스크리닝 대상 종목 없음");
            return List.of();
        }

        List<AnalysisResponse> analyses = tickers.stream()
                .map(ticker -> safeAnalyze(ticker))
                .filter(r -> r != null)
                .toList();

        List<ScreeningResult> results = screeningService.filter(analyses, request);
        log.info("[Screening] 완료 — 대상: {}종목, 통과: {}종목", tickers.size(), results.size());
        return results;
    }

    // ─────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────

    /** tickers 미지정 시 관심종목 전체를 대상으로 함 */
    private List<String> resolveTickers(ScreeningRequest request) {
        if (request.tickers() != null && !request.tickers().isEmpty()) {
            return request.tickers().stream()
                    .map(String::trim)
                    .filter(t -> !t.isBlank())
                    .toList();
        }
        return stockReader.findAllByIsWatchlistTrue().stream()
                .map(s -> s.getTicker())
                .toList();
    }

    /** 분석 실패 시 null 반환 — 해당 종목 skip (전체 실패 방지) */
    private AnalysisResponse safeAnalyze(String ticker) {
        try {
            return analysisUseCase.analyze(ticker);
        } catch (Exception e) {
            log.warn("[Screening] 분석 실패 skip — ticker: {}, reason: {}", ticker, e.getMessage());
            return null;
        }
    }
}
