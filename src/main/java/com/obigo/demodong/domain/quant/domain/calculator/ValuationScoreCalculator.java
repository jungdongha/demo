package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Feature: VALUATION_SCORE
 * P/E·P/B 기반 저평가도. 낮을수록(저평가) 점수 ↑.
 *
 * PER 정규화 (0~100):
 *   ≤ 0 (적자/없음) → 50 (중립)
 *   0 < per ≤ 10   → 100
 *   10 < per ≤ 30  → 선형 보간 (100 → 0)
 *   per > 30        → 0
 *
 * PBR 정규화 (0~100):
 *   ≤ 0 (없음) → 50 (중립)
 *   0 < pbr ≤ 1   → 100
 *   1 < pbr ≤ 3   → 선형 보간 (100 → 0)
 *   pbr > 3        → 0
 *
 * 최종: (perScore + pbrScore) / 2
 * rawValue: 실제 PER 값 (디버깅용, 적자/없음이면 0.0)
 * 가중치: 20%
 */
@Slf4j
@Component
public class ValuationScoreCalculator implements QuantFeatureCalculator {

    @Override
    public QuantFeatureResult calculate(QuantFeatureInput input) {
        QuantFundamentalData f = input.fundamentals();

        if (f == null) {
            log.debug("[Valuation] fundamentals 없음 - ticker={}, 중립 반환", input.ticker());
            return new QuantFeatureResult(getFeatureType(), 0.0, 50.0);
        }

        double perScore = scorePer(f.per());
        double pbrScore = scorePbr(f.pbr());
        double combined = (perScore + pbrScore) / 2.0;

        double rawPer = f.per() != null ? f.per().doubleValue() : 0.0;
        log.debug("[Valuation] ticker={}, per={}, pbr={}, score={:.1f}", input.ticker(), f.per(), f.pbr(), combined);

        return new QuantFeatureResult(getFeatureType(), rawPer, combined);
    }

    private double scorePer(BigDecimal per) {
        if (per == null || per.compareTo(BigDecimal.ZERO) <= 0) return 50.0;
        double v = per.doubleValue();
        if (v <= 10.0) return 100.0;
        if (v >= 30.0) return 0.0;
        // 10 < per < 30: 선형 보간 100 → 0
        return 100.0 - ((v - 10.0) / 20.0) * 100.0;
    }

    private double scorePbr(BigDecimal pbr) {
        if (pbr == null || pbr.compareTo(BigDecimal.ZERO) <= 0) return 50.0;
        double v = pbr.doubleValue();
        if (v <= 1.0) return 100.0;
        if (v >= 3.0) return 0.0;
        // 1 < pbr < 3: 선형 보간 100 → 0
        return 100.0 - ((v - 1.0) / 2.0) * 100.0;
    }

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.VALUATION_SCORE;
    }
}
