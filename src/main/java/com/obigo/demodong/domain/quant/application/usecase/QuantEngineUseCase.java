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
import com.obigo.demodong.domain.quant.domain.model.QuantScore;
import com.obigo.demodong.domain.quant.domain.port.QuantDataPort;
import com.obigo.demodong.domain.quant.domain.port.QuantNewsPort;
import com.obigo.demodong.domain.quant.domain.service.*;
import com.obigo.demodong.domain.stock.domain.enums.MarketType;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Quant Signal Engine 핵심 배치 UseCase.
 * 스케줄러(QuantSignalScheduler)에서 MarketType을 전달하여 호출한다.
 *
 * 처리 흐름:
 * 1. QuantUniverse에서 활성 종목 조회
 * 2. 시장 국면 판단 (MarketRegimeService)
 * 3. CRISIS 시 배치 중단
 * 4. 종목별 Feature 수집 + Score 계산
 * 5. Risk Filter 적용
 * 6. TOP3 선별
 * 7. LLM 해석 리포트 생성 (Groq)
 * 8. QuantSignal + QuantFeatureSnapshot 저장
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
    private final QuantNewsPort quantNewsPort;
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

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

        // 5. 종목별 Feature 수집 + Score 계산
        List<QuantScore> scores = universe.stream()
                .map(u -> calculateScore(u, indexReturn5d))
                .toList();

        // 6. Risk Filter 적용
        List<QuantScore> filtered = riskFilterService.filter(scores);

        // 7. TOP3 선별
        List<QuantScore> top3 = quantScoringService.selectTop3(filtered);
        log.info("[QuantEngine] TOP3 선별 완료: {}", top3.stream().map(QuantScore::ticker).toList());

        // 8. 저장 + LLM 리포트 생성
        for (int i = 0; i < top3.size(); i++) {
            QuantScore score = top3.get(i);
            saveSignalWithReport(score, i + 1);
        }

        log.info("[QuantEngine] 배치 완료 - market={}, top3={}", marketType, top3.size());
    }

    private QuantScore calculateScore(QuantUniverse universe, double indexReturn5d) {
        String ticker = universe.getTicker();
        MarketType market = universe.getMarketType();

        try {
            // 가격 데이터 수집
            List<DailyQuote> prices = quantDataPort.fetchRecentPrices(ticker, market, 10);

            // 뉴스 수집 (KOR: 회사명, USA: 티커)
            String newsQuery = market == MarketType.KOR ? universe.getStockName() : ticker;
            List<String> news = quantNewsPort.fetchNews(newsQuery, market);

            QuantFeatureInput input = new QuantFeatureInput(
                    ticker, universe.getStockName(), market, prices, news, indexReturn5d
            );

            return quantScoringService.score(input);

        } catch (Exception e) {
            log.warn("[QuantEngine] Feature 계산 실패 - ticker={}, error={}", ticker, e.getMessage());
            MarketRegime regime = marketRegimeService.analyze(indexReturn5d);
            return QuantScore.excluded(ticker, universe.getStockName(), market, regime,
                    "데이터 수집 실패: " + e.getMessage());
        }
    }

    private void saveSignalWithReport(QuantScore score, int rank) {
        // QuantSignal 저장
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

        // LLM 리포트 생성
        try {
            String report = generateLlmReport(score);
            signal.updateLlmReport(report);
            quantSignalWriter.save(signal);
        } catch (Exception e) {
            log.warn("[QuantEngine] LLM 리포트 생성 실패 - ticker={}: {}", score.ticker(), e.getMessage());
        }

        // QuantFeatureSnapshot 저장
        String rawJson = toRawJson(score.featureRawValues());
        quantSignalWriter.saveSnapshot(
                QuantFeatureSnapshot.create(
                        signal,
                        score.volumeRatio5d(),
                        score.priceMomentum5d(),
                        score.newsFreshness(),
                        score.featureRawValues().getOrDefault(
                                com.obigo.demodong.domain.quant.domain.enums.FeatureType.MARKET_REGIME, 0.0),
                        rawJson
                )
        );

        log.info("[QuantEngine] 시그널 저장 완료 - ticker={}, rank={}, score={:.1f}",
                score.ticker(), rank, score.totalScore());
    }

    private String generateLlmReport(QuantScore score) {
        String systemPrompt = "너는 퀀트 애널리스트다. 정량 지표를 바탕으로 종목 선정 이유를 간결하게 설명하라.";
        String userPrompt = String.format("""
                [%s(%s)] Quant 점수: %.1f점 — TOP%d 선정 이유를 3줄로 설명하라.

                [거래량 배수] %.2f배 (5일 평균 대비)
                [5일 모멘텀] %.2f%%
                [뉴스 신선도] %.1f점
                [시장 국면] %s

                출력 형식:
                ① {수급/모멘텀/뉴스 중 핵심 근거 1}
                ② {핵심 근거 2}
                ③ {핵심 근거 3}
                ⚠️ 본 시그널은 AI 참고 정보입니다. 투자 판단과 책임은 전적으로 사용자에게 있습니다.
                """,
                score.stockName(), score.ticker(), score.totalScore(),
                3,  // TOP 번호는 rank와 무관하게 고정 표시
                score.volumeRatio5d(),
                score.priceMomentum5d(),
                score.newsFreshness(),
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
