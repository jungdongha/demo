package com.obigo.demodong.domain.analysis.presentation;

import com.obigo.demodong.domain.analysis.application.dto.response.AnalysisResponse;
import com.obigo.demodong.domain.analysis.application.dto.response.StockSearchResponse;
import com.obigo.demodong.domain.analysis.application.usecase.AnalysisUseCase;
import com.obigo.demodong.domain.analysis.application.usecase.StockSearchUseCase;
import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;
import com.obigo.demodong.domain.analysis.domain.service.MarketRegimeService;
import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 종목 분석 REST API 컨트롤러.
 *
 * <p>엔드포인트:</p>
 * <ul>
 *   <li>GET /api/analysis/{ticker} — 7전략 + 기술/재무/수급 통합 분석</li>
 *   <li>GET /api/stocks            — 전체 종목 목록</li>
 *   <li>GET /api/stocks/search?q=  — 종목 검색</li>
 *   <li>GET /api/market/regime     — 현재 시장 국면</li>
 * </ul>
 */
@Tag(name = "분석 API", description = "7전략 퀀트 분석, 종목 검색, 시장 국면")
@Slf4j
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisUseCase analysisUseCase;
    private final StockSearchUseCase stockSearchUseCase;
    private final MarketRegimeService marketRegimeService;
    private final StockPricePort stockPricePort;

    /**
     * 종목 통합 분석.
     *
     * @param ticker 종목코드 (예: 005930, AAPL)
     */
    @Operation(summary = "종목 통합 분석", description = "7개 전략 점수 + 기술적/재무적/수급 분석 (KOR: DART+KIS, USA: 기술적 분석만)")
    @GetMapping("/analysis/{ticker}")
    public ApiResponse<AnalysisResponse> analyze(
            @Parameter(description = "종목코드 (예: 005930, AAPL)", example = "005930")
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9.\\-]{1,20}$", message = "종목코드 형식이 올바르지 않습니다.")
            String ticker) {
        log.info("[API] GET /api/analysis/{}", ticker);
        return ApiResponse.ok(
                AnalysisResponseCode.ANALYSIS_SUCCESS,
                analysisUseCase.analyze(ticker)
        );
    }

    /**
     * 전체 종목 목록 조회.
     * DB에 등록된 모든 종목 반환. 프론트 검색창 초기 데이터 / 자동완성 용도.
     */
    @Operation(summary = "전체 종목 목록", description = "DB에 등록된 전체 종목 반환 (캐시 24h, 프론트 자동완성 용도)")
    @Cacheable(value = "stocks", key = "'all'")
    @GetMapping("/stocks")
    public ApiResponse<List<StockSearchResponse>> getAllStocks() {
        log.info("[API] GET /api/stocks");
        return ApiResponse.ok(
                AnalysisResponseCode.STOCK_SEARCH_SUCCESS,
                stockSearchUseCase.findAll()
        );
    }

    /**
     * 종목 검색.
     *
     * @param q 검색어 (종목코드 / 영문 티커 / 한글 회사명)
     */
    @Operation(summary = "종목 검색", description = "종목코드 / 영문 티커 / 한글 회사명으로 검색")
    @GetMapping("/stocks/search")
    public ApiResponse<List<StockSearchResponse>> search(@Parameter(description = "검색어", example = "삼성") @RequestParam String q) {
        log.info("[API] GET /api/stocks/search?q={}", q);
        return ApiResponse.ok(
                AnalysisResponseCode.STOCK_SEARCH_SUCCESS,
                stockSearchUseCase.search(q)
        );
    }

    /**
     * 현재 시장 국면 조회.
     * KOSPI 지수 주가 기반 (STRONG_BULL / BULL / SIDEWAYS / BEAR / CRISIS).
     */
    @Operation(summary = "시장 국면 조회", description = "KOSPI 기반 시장 국면 (STRONG_BULL / BULL / SIDEWAYS / BEAR / CRISIS)")
    @Cacheable(value = "marketRegime", key = "'kospi'")
    @GetMapping("/market/regime")
    public ApiResponse<String> getMarketRegime() {
        log.info("[API] GET /api/market/regime");
        try {
            Stock kospi = analysisUseCase.resolveStock("0001");
            List<PriceSnapshot> prices = stockPricePort.fetchMonthlyPrices(kospi);
            List<BigDecimal> closes = prices.stream()
                    .map(PriceSnapshot::getClosePrice)
                    .toList();
            MarketRegime regime = marketRegimeService.determine(closes);
            return ApiResponse.ok(AnalysisResponseCode.MARKET_REGIME_SUCCESS, regime.name());
        } catch (Exception e) {
            log.warn("[API] 시장 국면 조회 실패: {}", e.getMessage());
            return ApiResponse.ok(AnalysisResponseCode.MARKET_REGIME_SUCCESS, MarketRegime.SIDEWAYS.name());
        }
    }
}
