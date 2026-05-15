package com.obigo.demodong.domain.quant.domain.service;

import com.obigo.demodong.domain.quant.domain.calculator.QuantFeatureCalculator;
import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import com.obigo.demodong.domain.quant.domain.model.QuantScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Feature 값에 가중치를 부여하여 0~100점 종합 Quant Score 산출.
 *
 * 가중치 (MVP 기본값):
 *   VOLUME_RATIO_5D:    35%
 *   PRICE_MOMENTUM_5D:  30%
 *   NEWS_FRESHNESS:     20%
 *   MARKET_REGIME:      직접 보정 (-10 ~ +10)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuantScoringService {

    private static final double WEIGHT_VOLUME   = 0.35;
    private static final double WEIGHT_MOMENTUM = 0.30;
    private static final double WEIGHT_NEWS     = 0.20;

    private final List<QuantFeatureCalculator> calculators;
    private final MarketRegimeService marketRegimeService;

    public QuantScore score(QuantFeatureInput input) {
        // 각 Calculator 실행
        Map<FeatureType, QuantFeatureResult> results = calculators.stream()
                .collect(Collectors.toMap(
                        QuantFeatureCalculator::getFeatureType,
                        c -> c.calculate(input)
                ));

        double volumeScore    = getScore(results, FeatureType.VOLUME_RATIO_5D);
        double momentumScore  = getScore(results, FeatureType.PRICE_MOMENTUM_5D);
        double newsScore      = getScore(results, FeatureType.NEWS_FRESHNESS);
        double regimeBonus    = getRaw(results, FeatureType.MARKET_REGIME);

        double totalScore = (volumeScore * WEIGHT_VOLUME)
                + (momentumScore * WEIGHT_MOMENTUM)
                + (newsScore * WEIGHT_NEWS)
                + regimeBonus;

        // 0~100 범위 보정
        totalScore = Math.max(0.0, Math.min(100.0, totalScore));

        MarketRegime regime = marketRegimeService.analyze(input.indexReturn5d());

        Map<FeatureType, Double> featureRawValues = Map.of(
                FeatureType.VOLUME_RATIO_5D,   getRaw(results, FeatureType.VOLUME_RATIO_5D),
                FeatureType.PRICE_MOMENTUM_5D, getRaw(results, FeatureType.PRICE_MOMENTUM_5D),
                FeatureType.NEWS_FRESHNESS,    getRaw(results, FeatureType.NEWS_FRESHNESS),
                FeatureType.MARKET_REGIME,     regimeBonus
        );

        log.info("[Scoring] ticker={}, total={:.1f} (vol={:.1f}, mom={:.1f}, news={:.1f}, regime={})",
                input.ticker(), totalScore, volumeScore, momentumScore, newsScore, regime);

        return new QuantScore(
                input.ticker(),
                input.stockName(),
                input.market(),
                totalScore,
                featureRawValues,
                getRaw(results, FeatureType.VOLUME_RATIO_5D),
                getRaw(results, FeatureType.PRICE_MOMENTUM_5D),
                newsScore,
                regime,
                false,
                null,
                LocalDateTime.now()
        );
    }

    public List<QuantScore> selectTop3(List<QuantScore> filtered) {
        return filtered.stream()
                .filter(s -> !s.excluded())
                .sorted(Comparator.comparingDouble(QuantScore::totalScore).reversed())
                .limit(3)
                .toList();
    }

    private double getScore(Map<FeatureType, QuantFeatureResult> results, FeatureType type) {
        QuantFeatureResult r = results.get(type);
        return r != null ? r.normalizedScore() : 0.0;
    }

    private double getRaw(Map<FeatureType, QuantFeatureResult> results, FeatureType type) {
        QuantFeatureResult r = results.get(type);
        return r != null ? r.rawValue() : 0.0;
    }
}
