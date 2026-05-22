package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import com.obigo.demodong.domain.quant.domain.model.QuantFundamentalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Feature: TARGET_PRICE_UPSIDE
 * 증권사 평균 목표주가 대비 현재가 상승 여력 %.
 * 여력이 클수록 점수 ↑.
 *
 * 공식:
 *   upside% = (targetPrice - currentPrice) / currentPrice * 100
 *   normalized = clamp(upside% / 30.0 * 100, 0, 100)
 *   (30% 상승 여력 → 만점, 0% 이하 → 0점)
 *
 * 데이터 없음(targetPrice=null or currentPrice≤0): 50점 중립 반환.
 *
 * rawValue: upside% (실제 상승 여력 %, 스냅샷 저장용)
 * 가중치: 15%
 *
 * Phase 10 TODO: Naver Finance 스크래핑으로 targetPrice 채우기
 */
@Slf4j
@Component
public class TargetPriceUpsideCalculator implements QuantFeatureCalculator {

    private static final double FULL_SCORE_UPSIDE_PCT = 30.0;  // 30% 상승 여력 → 100점

    @Override
    public QuantFeatureResult calculate(QuantFeatureInput input) {
        QuantFundamentalData f = input.fundamentals();

        if (f == null || f.targetPrice() == null) {
            log.debug("[TargetUpside] 목표주가 없음(Phase10 Stub) - ticker={}, 중립 반환", input.ticker());
            return new QuantFeatureResult(getFeatureType(), 0.0, 50.0);
        }

        BigDecimal currentPrice = f.currentPrice();
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("[TargetUpside] 현재가 없음 - ticker={}, 중립 반환", input.ticker());
            return new QuantFeatureResult(getFeatureType(), 0.0, 50.0);
        }

        double upside = f.targetPrice().subtract(currentPrice)
                .divide(currentPrice, 4, java.math.RoundingMode.HALF_UP)
                .doubleValue() * 100.0;

        double normalized = Math.min(Math.max(upside / FULL_SCORE_UPSIDE_PCT * 100.0, 0.0), 100.0);

        log.debug("[TargetUpside] ticker={}, upside={}%, score={:.1f}", input.ticker(), upside, normalized);
        return new QuantFeatureResult(getFeatureType(), upside, normalized);
    }

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.TARGET_PRICE_UPSIDE;
    }
}
