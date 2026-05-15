package com.obigo.demodong.domain.quant.domain.calculator;

import com.obigo.demodong.domain.quant.domain.enums.FeatureType;
import com.obigo.demodong.domain.quant.domain.model.DailyQuote;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureInput;
import com.obigo.demodong.domain.quant.domain.model.QuantFeatureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Feature: price_momentum_5d
 * 5일 가격 모멘텀 = (현재가 - 5일 전 종가) / 5일 전 종가 * 100 (%).
 *
 * 정규화: ±10% 범위를 0~100으로 정규화
 *   normalized = min(max(momentum + 10, 0), 20) / 20 * 100
 * 가중치: 30%
 */
@Slf4j
@Component
public class PriceMomentum5dCalculator implements QuantFeatureCalculator {

    @Override
    public QuantFeatureResult calculate(QuantFeatureInput input) {
        List<DailyQuote> prices = input.priceHistory();

        if (prices == null || prices.size() < 2) {
            log.warn("[PriceMomentum5d] 가격 데이터 부족 - ticker: {}", input.ticker());
            return new QuantFeatureResult(getFeatureType(), 0.0, 50.0);  // 중립값
        }

        BigDecimal currentPrice = prices.get(0).closePrice();
        BigDecimal pastPrice = prices.get(Math.min(prices.size() - 1, 4)).closePrice(); // 5일 전 or 마지막

        if (pastPrice == null || pastPrice.compareTo(BigDecimal.ZERO) == 0 || currentPrice == null) {
            return new QuantFeatureResult(getFeatureType(), 0.0, 50.0);
        }

        double momentum = currentPrice.subtract(pastPrice)
                .divide(pastPrice, 4, java.math.RoundingMode.HALF_UP)
                .doubleValue() * 100.0;

        // ±10% 범위 정규화 → 0~100
        double normalized = Math.min(Math.max(momentum + 10.0, 0.0), 20.0) / 20.0 * 100.0;

        log.debug("[PriceMomentum5d] ticker={}, momentum={}%, score={}", input.ticker(), momentum, normalized);
        return new QuantFeatureResult(getFeatureType(), momentum, normalized);
    }

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.PRICE_MOMENTUM_5D;
    }
}
