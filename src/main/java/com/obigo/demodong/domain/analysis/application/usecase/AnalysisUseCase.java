package com.obigo.demodong.domain.analysis.application.usecase;

import com.obigo.demodong.domain.analysis.application.dto.response.*;
import com.obigo.demodong.domain.analysis.domain.calculator.StrategyCalculator;
import com.obigo.demodong.domain.analysis.domain.enums.MarketRegime;
import com.obigo.demodong.domain.analysis.domain.enums.StrategyType;
import com.obigo.demodong.domain.analysis.domain.model.MomentumSnapshot;
import com.obigo.demodong.domain.analysis.domain.model.StrategyInput;
import com.obigo.demodong.domain.analysis.domain.model.StrategyScore;
import com.obigo.demodong.domain.analysis.domain.service.MarketRegimeService;
import com.obigo.demodong.domain.flow.domain.model.FlowSnapshot;
import com.obigo.demodong.domain.flow.domain.port.FlowDataPort;
import com.obigo.demodong.domain.fundamental.domain.model.FundamentalSnapshot;
import com.obigo.demodong.domain.fundamental.domain.port.FundamentalDataPort;
import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.price.domain.port.StockPricePort;
import com.obigo.demodong.domain.price.infrastructure.dart.DartCorpCodeMapper;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.domain.stock.domain.service.StockReader;
import com.obigo.demodong.domain.stock.domain.service.StockWriter;
import com.obigo.demodong.domain.technical.application.usecase.TechnicalAnalysisUseCase;
import com.obigo.demodong.domain.technical.domain.calculator.ReturnCalculator;
import com.obigo.demodong.domain.technical.domain.calculator.RsRatingCalculator;
import com.obigo.demodong.domain.technical.domain.model.TechnicalSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * 종목 통합 분석 유스케이스.
 *
 * <p>7개 전략 Calculator를 오케스트레이션하여 {@link AnalysisResponse}를 반환한다.</p>
 *
 * <p>데이터 흐름:</p>
 * <ol>
 *   <li>Stock 조회 또는 자동 생성</li>
 *   <li>주가 200일 조회 → TechnicalSnapshot 계산</li>
 *   <li>FundamentalSnapshot 조회 (KOR: DART+KIS, USA: stub)</li>
 *   <li>FlowSnapshot 조회 (KOR: KIS 수급, USA: stub)</li>
 *   <li>MomentumSnapshot 계산 (수익률 + RS Rating)</li>
 *   <li>MarketRegime 판단</li>
 *   <li>7개 Calculator 병렬 실행</li>
 *   <li>AnalysisResponse 조립</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisUseCase {

    /** OCP: 7개 StrategyCalculator Bean 자동 수집 — 새 전략 추가 시 이 클래스 수정 불필요 */
    private final List<StrategyCalculator> calculators;

    private final StockReader stockReader;
    private final StockWriter stockWriter;
    private final StockPricePort stockPricePort;
    private final TechnicalAnalysisUseCase technicalAnalysisUseCase;
    private final FundamentalDataPort fundamentalDataPort;
    private final FlowDataPort flowDataPort;
    private final MarketRegimeService marketRegimeService;
    private final ReturnCalculator returnCalculator;
    private final RsRatingCalculator rsRatingCalculator;
    private final DartCorpCodeMapper dartCorpCodeMapper;

    /** 시장 지수 대리: KOSPI 지수 종목코드 */
    private static final String KOSPI_TICKER = "0001";

    /**
     * 종목 통합 분석을 수행한다.
     *
     * @param ticker 종목코드 (6자리 KOR / 영문 USA)
     * @return 7전략 + 기술/재무/수급 통합 분석 결과
     */
    @Cacheable(value = "analysis", key = "#ticker")
    @Transactional
    public AnalysisResponse analyze(String ticker) {
        log.info("[Analysis] 분석 시작 - ticker: {}", ticker);

        // 1. Stock 조회 (없으면 자동 생성)
        Stock stock = resolveStock(ticker);

        // 2. 주가 데이터 조회 (200거래일 + α)
        List<PriceSnapshot> priceSnapshots = fetchPrices(stock);
        List<BigDecimal> closePrices = priceSnapshots.stream()
                .map(PriceSnapshot::getClosePrice)
                .toList();

        // 3. 52주 고저가
        Optional<BigDecimal[]> weekRange52 = stockPricePort.fetch52WeekRange(stock);
        BigDecimal high52w = weekRange52.map(r -> r[0]).orElse(null);
        BigDecimal low52w  = weekRange52.map(r -> r[1]).orElse(null);

        // 3.5. 실시간 현재가 조회 (실패 시 null → TechnicalResponse에서 마지막 종가로 fallback)
        BigDecimal realTimePrice = null;
        try {
            realTimePrice = stockPricePort.fetchCurrentPrice(stock);
        } catch (Exception e) {
            log.debug("[Analysis] 실시간 현재가 조회 실패, 마지막 종가 사용 - ticker: {}", ticker);
        }

        // 4. 기술적 분석
        TechnicalSnapshot technical = technicalAnalysisUseCase.analyze(closePrices, high52w, low52w);

        // 5. 재무 분석 (KOR: DART+KIS 병합, USA: stub)
        FundamentalSnapshot fundamental = fundamentalDataPort.fetch(
                stock.getTicker(), stock.getDartCorpCode(), stock.getMarketType());

        // 6. 수급 분석 (KOR: KIS, USA: stub)
        FlowSnapshot flow = flowDataPort.fetch(stock.getTicker(), stock.getMarketType());

        // 6.5. 거래량 통계 계산 (PriceSnapshot.volume 활용, nullable 안전 처리)
        BigDecimal volumeChangeRate = calculateVolumeChangeRate(priceSnapshots);
        Long tradingValue = calculateTradingValue(priceSnapshots);

        // 7. 모멘텀 계산
        MomentumSnapshot momentum = buildMomentum(closePrices, stock.getMarketType());

        // 8. 시장 국면 판단 (KOR: KOSPI 기반, USA: 주가 자체 사용)
        MarketRegime regime = determineRegime(stock, closePrices);

        // 9. StrategyInput 조립
        StrategyInput input = new StrategyInput(
                stock.getTicker(), stock.getMarketType(),
                technical, momentum, regime, stock.getSector(),
                fundamental, flow
        );

        // 10. 7개 전략 병렬 계산
        List<StrategyScoreResponse> strategies = calculators.parallelStream()
                .map(calc -> safeCalculate(calc, input))
                .map(StrategyScoreResponse::from)
                .toList();

        // 11. DTO 조립
        TechnicalResponse technicalResponse = TechnicalResponse.from(
                technical,
                realTimePrice,
                momentum.return1m(), momentum.return3m(),
                momentum.return6m(), momentum.return12m(),
                momentum.rsRating()
        );

        log.info("[Analysis] 분석 완료 - ticker: {}, 전략수: {}", ticker, strategies.size());

        return new AnalysisResponse(
                stock.getTicker(),
                stock.getName(),
                stock.getMarketType().name(),
                stock.getSector(),
                LocalDateTime.now(),
                strategies,
                technicalResponse,
                FundamentalResponse.from(fundamental),
                FlowResponse.from(flow, tradingValue, volumeChangeRate),
                regime != null ? regime.name() : MarketRegime.SIDEWAYS.name()
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Stock을 조회하거나 신규 생성한다.
     * KOR: 숫자 (4자리 지수코드 포함), USA: 영문. 생성 시 DART 법인코드 자동 매핑 시도.
     */
    @Transactional
    public Stock resolveStock(String ticker) {
        return stockReader.findByTicker(ticker).orElseGet(() -> {
            MarketType marketType = ticker.matches("\\d+") ? MarketType.KOR : MarketType.USA;
            Stock newStock = stockWriter.save(
                    Stock.builder()
                            .ticker(ticker)
                            .name(ticker)   // 이름 미확인 시 ticker로 대체
                            .marketType(marketType)
                            .isWatchlist(false)
                            .build()
            );
            // KOR이면 종목명 + DART 법인코드 자동 매핑 시도
            if (marketType == MarketType.KOR) {
                String stockName = stockPricePort.fetchStockName(newStock);
                newStock.updateName(stockName);
                dartCorpCodeMapper.resolveCorpCodeByTicker(ticker)
                        .ifPresent(newStock::updateDartCorpCode);
            }
            log.info("[Analysis] 신규 종목 생성 - ticker: {}, market: {}", ticker, marketType);
            return newStock;
        });
    }

    /** 주가 조회 — 빈 리스트 반환 시 하위 계산기가 null/중립으로 처리 */
    private List<PriceSnapshot> fetchPrices(Stock stock) {
        try {
            return stockPricePort.fetchMonthlyPrices(stock);
        } catch (Exception e) {
            log.warn("[Analysis] 주가 조회 실패 - ticker: {}, reason: {}", stock.getTicker(), e.getMessage());
            return List.of();
        }
    }

    /** 모멘텀 스냅샷 계산 (수익률 1/3/6/12개월 + RS Rating) */
    private MomentumSnapshot buildMomentum(List<BigDecimal> closePrices, MarketType market) {
        BigDecimal ret1m  = returnCalculator.calculate(closePrices, 20);
        BigDecimal ret3m  = returnCalculator.calculate(closePrices, 60);
        BigDecimal ret6m  = returnCalculator.calculate(closePrices, 120);
        BigDecimal ret12m = returnCalculator.calculate(closePrices, 240);

        // RS Rating: 시장 수익률 기준 (간소화: 시장 대리 없으면 중립 50)
        int rsRating = 50;
        if (market == MarketType.KOR) {
            try {
                Stock kospi = resolveStock(KOSPI_TICKER);
                List<PriceSnapshot> indexPrices = fetchPrices(kospi);
                List<BigDecimal> indexClose = indexPrices.stream()
                        .map(PriceSnapshot::getClosePrice).toList();
                BigDecimal marketReturn12m = returnCalculator.calculate(indexClose, 240);
                rsRating = rsRatingCalculator.calculate(ret12m, marketReturn12m);
            } catch (Exception e) {
                log.debug("[Analysis] RS Rating KOSPI 조회 실패 — 중립 사용: {}", e.getMessage());
            }
        }

        return new MomentumSnapshot(ret1m, ret3m, ret6m, ret12m, rsRating);
    }

    /** 시장 국면 판단 */
    private MarketRegime determineRegime(Stock stock, List<BigDecimal> closePrices) {
        try {
            if (stock.getMarketType() == MarketType.KOR) {
                Stock kospi = resolveStock(KOSPI_TICKER);
                List<PriceSnapshot> indexPrices = fetchPrices(kospi);
                List<BigDecimal> indexClose = indexPrices.stream()
                        .map(PriceSnapshot::getClosePrice).toList();
                return marketRegimeService.determine(indexClose);
            }
            // USA: 종목 자체 주가로 대리
            return marketRegimeService.determine(closePrices);
        } catch (Exception e) {
            log.debug("[Analysis] 시장 국면 판단 실패 — SIDEWAYS 사용: {}", e.getMessage());
            return MarketRegime.SIDEWAYS;
        }
    }

    /**
     * 거래량 증가율 계산 — 최근 5일 평균 거래량 / 최근 20일 평균 거래량 - 1 (%)
     * priceSnapshots는 오래된 순서(오름차순). volume nullable → null 안전 처리.
     */
    private BigDecimal calculateVolumeChangeRate(List<PriceSnapshot> snapshots) {
        int size = snapshots.size();
        if (size < 20) return null;

        List<PriceSnapshot> recent5  = snapshots.subList(size - 5, size);
        List<PriceSnapshot> recent20 = snapshots.subList(size - 20, size);

        OptionalDouble avg5  = recent5.stream()
                .filter(p -> p.getVolume() != null)
                .mapToLong(PriceSnapshot::getVolume)
                .average();
        OptionalDouble avg20 = recent20.stream()
                .filter(p -> p.getVolume() != null)
                .mapToLong(PriceSnapshot::getVolume)
                .average();

        if (avg5.isEmpty() || avg20.isEmpty() || avg20.getAsDouble() == 0) return null;

        double rate = (avg5.getAsDouble() / avg20.getAsDouble() - 1) * 100;
        return BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 최근 거래대금 계산 — 가장 최근 종가 × 거래량 (원 단위).
     */
    private Long calculateTradingValue(List<PriceSnapshot> snapshots) {
        if (snapshots.isEmpty()) return null;
        PriceSnapshot latest = snapshots.get(snapshots.size() - 1);
        if (latest.getVolume() == null) return null;
        return latest.getClosePrice()
                .multiply(BigDecimal.valueOf(latest.getVolume()))
                .longValue();
    }

    /**
     * 전략 계산 — 예외 발생 시 50점 중립 반환.
     * 단일 전략 오류가 전체 응답을 깨지 않도록 방어.
     */
    private StrategyScore safeCalculate(StrategyCalculator calc, StrategyInput input) {
        try {
            return calc.calculate(input);
        } catch (Exception e) {
            StrategyType type = calc.getSupportedStrategy();
            log.warn("[Analysis] {} 전략 계산 실패: {}", type, e.getMessage());
            return new StrategyScore(type, 50, "B", java.util.Map.of(),
                    java.util.List.of(), java.util.List.of("계산 오류: " + e.getMessage()));
        }
    }
}
