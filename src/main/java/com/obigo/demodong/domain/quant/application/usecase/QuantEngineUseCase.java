package com.obigo.demodong.domain.quant.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.obigo.demodong.domain.ai.infrastructure.service.AiChatService;
import com.obigo.demodong.domain.quant.application.exception.QuantErrorCode;
import com.obigo.demodong.domain.quant.domain.entity.QuantFeatureSnapshot;
import com.obigo.demodong.domain.quant.domain.entity.QuantSignal;
import com.obigo.demodong.domain.quant.domain.entity.QuantUniverse;
import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import com.obigo.demodong.domain.quant.domain.model.QuantScore;
import com.obigo.demodong.domain.quant.domain.port.QuantDataPort;
import com.obigo.demodong.domain.quant.domain.port.QuantFundamentalPort;
import com.obigo.demodong.domain.quant.domain.port.QuantNewsPort;
import com.obigo.demodong.domain.quant.domain.service.*;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quant Signal Engine 핵심 배치 UseCase.
 * 스케줄러(QuantSignalScheduler)에서 MarketType을 전달하여 호출한다.
 *
 * 처리 흐름 (2-pass):
 * [Pass 1] QuantUniverse 전체 종목 데이터 수집 (가격 + 펀더멘털 + 뉴스)
 * [집계]   섹터별 평균 5일 모멘텀 사전 계산
 * [Pass 2] 종목별 QuantFeatureInput 조립 → 7개 Feature Score 계산
 *          → Risk Filter → TOP3 선별 → LLM 리포트 → 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuantEngineUseCase {

    private final QuantUniverseReader quantUniverseReader;
    private final QuantSignalReader quantSignalReader;
    private final QuantSignalWriter quantSignalWriter;
    private final QuantScoringService quantScoringService;
    private final RiskFilterService riskFilterService;
    private final MarketRegimeService marketRegimeService;
    private final QuantDataPort quantDataPort;
    private final QuantFundamentalPort quantFundamentalPort;
    private final QuantNewsPort quantNewsPort;
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    /** Pass 1 수집 결과를 담는 내부 레코드 */
    private record StockData(
            QuantUniverse universe,
            List<DailyQuote> prices,
            QuantFundamentalData fundamentals,
            List<String> news
    ) {}

    @Transactional
    public void runBatch(MarketType marketType) {
        log.info("[QuantEngine] 배치 시작 - market={}", marketType);

        // 1. 유니버스 조회
        List<QuantUniverse> universe = quantUniverseReader.findActiveByMarket(marketType);
        if (universe.isEmpty()) {
            throw new ApplicationException(QuantErrorCode.QUANT_UNIVERSE_EMPTY);
        }

        // 2. 시장 국면 판단
        double indexReturn5d = quantDataPort.fetchIndexReturn5d(marketType);
        MarketRegime regime = marketRegimeService.analyze(indexReturn5d);
        log.info("[QuantEngine] 시장 국면={}, 지수5일변동={}%", regime, indexReturn5d);

        // 3. CRISIS 시 배치 중단
        if (regime == MarketRegime.CRISIS) {
            log.warn("[QuantEngine] CRISIS 국면 — 시그널 생성 중단");
            throw new ApplicationException(QuantErrorCode.MARKET_REGIME_CRISIS);
        }

        // 4. 당일 기존 시그널 삭제 (덮어쓰기)
        List<QuantSignal> existing = quantSignalReader.findTodayByMarket(marketType);
        if (!existing.isEmpty()) {
            quantSignalWriter.deleteAll(existing);
            log.info("[QuantEngine] 기존 당일 시그널 {}건 삭제 (재실행)", existing.size());
        }

        // ── Pass 1: 전체 종목 데이터 수집 ───────────────────────────────────
        List<StockData> allData = collectAllData(universe, marketType);

        // ── 섹터 평균 모멘텀 사전 집계 ────────────────────────────────────
        Map<String, Double> sectorAvgMomentums = computeSectorAvgMomentums(allData);
        log.info("[QuantEngine] 섹터 평균 모멘텀 집계 완료: {}", sectorAvgMomentums);

        // ── Pass 2: 종목별 Feature 계산 ───────────────────────────────────
        List<QuantScore> scores = allData.stream()
                .map(data -> calculateScore(data, indexReturn5d, sectorAvgMomentums))
                .toList();

        // 5. Risk Filter 적용
        List<QuantScore> filtered = riskFilterService.filter(scores);

        // 6. TOP3 선별
        List<QuantScore> top3 = quantScoringService.selectTop3(filtered);
        log.info("[QuantEngine] TOP3 선별 완료: {}", top3.stream().map(QuantScore::ticker).toList());

        // 7. 저장 + LLM 리포트 생성
        for (int i = 0; i < top3.size(); i++) {
            saveSignalWithReport(top3.get(i), i + 1);
        }

        log.info("[QuantEngine] 배치 완료 - market={}, top3={}", marketType, top3.size());
    }

    // ── Pass 1: 전체 데이터 수집 ─────────────────────────────────────────

    private List<StockData> collectAllData(List<QuantUniverse> universe, MarketType marketType) {
        List<StockData> result = new ArrayList<>();
        for (QuantUniverse u : universe) {
            try {
                List<DailyQuote> prices = quantDataPort.fetchRecentPrices(u.getTicker(), marketType, 10);
                QuantFundamentalData fundamentals = quantFundamentalPort.fetchFundamentals(u.getTicker(), marketType);
                String newsQuery = marketType == MarketType.KOR ? u.getStockName() : u.getTicker();
                List<String> news = quantNewsPort.fetchNews(newsQuery, marketType);
                result.add(new StockData(u, prices, fundamentals, news));
            } catch (Exception e) {
                log.warn("[QuantEngine] Pass1 데이터 수집 실패 - ticker={}, error={}", u.getTicker(), e.getMessage());
                result.add(new StockData(u, List.of(), QuantFundamentalData.stub(BigDecimal.ZERO), List.of()));
            }
        }
        return result;
    }

    // ── 섹터 평균 모멘텀 집계 ─────────────────────────────────────────────

    private Map<String, Double> computeSectorAvgMomentums(List<StockData> allData) {
        Map<String, List<Double>> sectorMomentums = new HashMap<>();
        for (StockData data : allData) {
            String sector = data.universe().getSector();
            if (sector == null || sector.isBlank()) continue;
            sectorMomentums.computeIfAbsent(sector, k -> new ArrayList<>())
                    .add(computeMomentum5d(data.prices()));
        }
        return sectorMomentums.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                ));
    }

    private double computeMomentum5d(List<DailyQuote> prices) {
        if (prices == null || prices.size() < 2) return 0.0;
        BigDecimal current = prices.get(0).closePrice();
        BigDecimal past = prices.get(Math.min(prices.size() - 1, 4)).closePrice();
        if (past == null || past.compareTo(BigDecimal.ZERO) == 0 || current == null) return 0.0;
        return current.subtract(past).divide(past, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
    }

    // ── Pass 2: 종목별 Score 계산 ─────────────────────────────────────────

    private QuantScore calculateScore(StockData data, double indexReturn5d,
                                      Map<String, Double> sectorAvgMomentums) {
        String ticker = data.universe().getTicker();
        MarketType market = data.universe().getMarketType();
        String sector = data.universe().getSector();

        try {
            double sectorAvg = (sector != null && !sector.isBlank())
                    ? sectorAvgMomentums.getOrDefault(sector, 0.0)
                    : 0.0;

            QuantFeatureInput input = new QuantFeatureInput(
                    ticker,
                    data.universe().getStockName(),
                    market,
                    data.prices(),
                    data.news(),
                    indexReturn5d,
                    data.fundamentals(),
                    sectorAvg
            );

            return quantScoringService.score(input);

        } catch (Exception e) {
            log.warn("[QuantEngine] Feature 계산 실패 - ticker={}, error={}", ticker, e.getMessage());
            MarketRegime regime = marketRegimeService.analyze(indexReturn5d);
            return QuantScore.excluded(ticker, data.universe().getStockName(), market, regime,
                    "데이터 수집 실패: " + e.getMessage());
        }
    }

    // ── LLM 리포트 생성 + 저장 ────────────────────────────────────────────

    private void saveSignalWithReport(QuantScore score, int rank) {
        QuantSignal signal = quantSignalWriter.save(
                QuantSignal.create(
                        score.ticker(),
                        score.stockName(),
                        score.marketType(),
                        score.totalScore(),
                        rank,
                        score.marketRegime(),
                        LocalDate.now()
                )
        );

        try {
            String report = generateLlmReport(score, rank);
            signal.updateLlmReport(report);
            quantSignalWriter.save(signal);
        } catch (Exception e) {
            log.warn("[QuantEngine] LLM 리포트 생성 실패 - ticker={}: {}", score.ticker(), e.getMessage());
        }

        String rawJson = toRawJson(score.featureRawValues());
        quantSignalWriter.saveSnapshot(QuantFeatureSnapshot.create(signal, score, rawJson));

        log.info("[QuantEngine] 시그널 저장 완료 - ticker={}, rank={}, score={:.1f}",
                score.ticker(), rank, score.totalScore());
    }

    private String generateLlmReport(QuantScore score, int rank) {
        String systemPrompt = "너는 퀀트 애널리스트다. 정량 지표를 바탕으로 종목 선정 이유를 간결하게 설명하라.";
        String userPrompt = String.format("""
                [%s(%s)] Quant 점수: %.1f점 — TOP%d 선정 이유를 3줄로 설명하라.

                [거래량 배수]    %.2f배 (5일 평균 대비)
                [5일 모멘텀]     %.2f%%
                [뉴스 신선도]    %.1f점
                [밸류에이션]     %.1f점 (PER=%.1f, PBR=%.2f)
                [목표주가 여력]  %.1f점 (목표주가=%s)
                [섹터 상대강도]  %.1f점
                [시장 국면]      %s

                출력 형식:
                ① {수급/모멘텀/밸류에이션/컨센서스/섹터 중 핵심 근거 1}
                ② {핵심 근거 2}
                ③ {핵심 근거 3}
                ⚠️ 본 시그널은 AI 참고 정보입니다. 투자 판단과 책임은 전적으로 사용자에게 있습니다.
                """,
                score.stockName(), score.ticker(), score.totalScore(), rank,
                score.volumeRatio5d(),
                score.priceMomentum5d(),
                score.newsFreshness(),
                score.valuationScore(),
                score.fundamentals() != null && score.fundamentals().per() != null
                        ? score.fundamentals().per().doubleValue() : 0.0,
                score.fundamentals() != null && score.fundamentals().pbr() != null
                        ? score.fundamentals().pbr().doubleValue() : 0.0,
                score.targetPriceUpsideScore(),
                score.fundamentals() != null && score.fundamentals().targetPrice() != null
                        ? score.fundamentals().targetPrice().toPlainString() : "미제공",
                score.sectorRelativeScore(),
                score.marketRegime()
        );

        return aiChatService.getChatResponse(systemPrompt, userPrompt);
    }

    private String toRawJson(Map<?, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
