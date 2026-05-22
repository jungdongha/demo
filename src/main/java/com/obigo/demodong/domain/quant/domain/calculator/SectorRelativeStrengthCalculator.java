package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Feature: SECTOR_RELATIVE_STRENGTH
 * 유니버스 내 동일 섹터 평균 5일 수익률 대비 현재 종목의 상대 강도.
 * 섹터 평균보다 강하면 점수 ↑.
 *
 * 공식:
 *   selfMomentum5d   = (현재가 - 5일전가) / 5일전가 * 100 (%)
 *   relative         = selfMomentum5d - input.sectorAvgMomentum5d()
 *   normalized       = clamp((relative + 10) / 20.0 * 100, 0, 100)
 *   (relative +10%p → 100점, -10%p → 0점)
 *
 * 섹터 내 종목 1개(비교 불가, sectorAvg = self)일 때 → relative=0 → 50점 (자동 중립).
 * 가격 데이터 부족 → 50점 중립 반환.
 *
 * rawValue: relative momentum% (스냅샷 저장용)
 * 가중치: 15%
 */
@Slf4j
@Component
public class SectorRelativeStrengthCalculator implements QuantFeatureCalculator {

    @Override
    public QuantFeatureResult calculate(QuantFeatureInput input) {
        List<DailyQuote> prices = input.priceHistory();

        if (prices == null || prices.size() < 2) {
            log.debug("[SectorRelative] 가격 데이터 부족 - ticker={}, 중립 반환", input.ticker());
            return new QuantFeatureResult(getFeatureType(), 0.0, 50.0);
        }

        double selfMomentum = computeMomentum5d(prices);
        double relative = selfMomentum - input.sectorAvgMomentum5d();
        double normalized = Math.min(Math.max((relative + 10.0) / 20.0 * 100.0, 0.0), 100.0);

        log.debug("[SectorRelative] ticker={}, selfMom={}%, sectorAvg={}%, relative={}%, score={:.1f}",
                input.ticker(), selfMomentum, input.sectorAvgMomentum5d(), relative, normalized);

        return new QuantFeatureResult(getFeatureType(), relative, normalized);
    }

    /** PriceMomentum5dCalculator와 동일한 공식 — Calculator 간 결합 방지를 위해 인라인 유지 */
    private double computeMomentum5d(List<DailyQuote> prices) {
        BigDecimal current = prices.get(0).closePrice();
        BigDecimal past = prices.get(Math.min(prices.size() - 1, 4)).closePrice();
        if (past == null || past.compareTo(BigDecimal.ZERO) == 0 || current == null) return 0.0;
        return current.subtract(past)
                .divide(past, 4, RoundingMode.HALF_UP)
                .doubleValue() * 100.0;
    }

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.SECTOR_RELATIVE_STRENGTH;
    }
}
