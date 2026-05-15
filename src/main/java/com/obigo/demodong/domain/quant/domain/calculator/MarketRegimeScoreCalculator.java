package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.enums.MarketRegime;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import com.obigo.demodong.domain.quant.domain.service.MarketRegimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Feature: market_regime_bonus
 * 시장 국면에 따른 보정 계수를 반환한다 (-10 ~ +10).
 * rawValue = 보정 계수, normalizedScore = 동일 (직접 가산)
 *
 * 가중치: 직접 totalScore에 가산 (비율 가중치 외 보정)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketRegimeScoreCalculator implements QuantFeatureCalculator {

    private final MarketRegimeService marketRegimeService;

    @Override
    public QuantFeatureResult calculate(QuantFeatureInput input) {
        MarketRegime regime = marketRegimeService.analyze(input.indexReturn5d());

        double bonus = switch (regime) {
            case STRONG_BULL -> 10.0;
            case BULL        -> 5.0;
            case SIDEWAYS    -> 0.0;
            case BEAR        -> -5.0;
            case CRISIS      -> -10.0;
        };

        log.debug("[MarketRegime] ticker={}, regime={}, bonus={}", input.ticker(), regime, bonus);
        return new QuantFeatureResult(getFeatureType(), bonus, bonus);
    }

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.MARKET_REGIME;
    }
}
