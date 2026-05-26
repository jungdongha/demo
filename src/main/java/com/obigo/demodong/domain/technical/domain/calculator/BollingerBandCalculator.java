package com.obigo.demodong.domain.technical.domain.calculator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 볼린저 밴드 계산기.
 * Mid    = SMA20
 * Upper  = SMA20 + 2σ
 * Lower  = SMA20 - 2σ
 */
@Component
@RequiredArgsConstructor
public class BollingerBandCalculator {

    private static final int PERIOD     = 20;
    private static final int MULTIPLIER = 2;
    private static final int SCALE      = 10;

    private final SmaCalculator smaCalculator;

    /**
     * @param prices 종가 리스트 (오래된 순서 → 최근 순서). 최소 20개 필요.
     * @return BollingerResult. 데이터 부족 시 null.
     */
    public BollingerResult calculate(List<BigDecimal> prices) {
        if (prices == null || prices.size() < PERIOD) {
            return null;
        }

        BigDecimal mid = smaCalculator.calculate(prices, PERIOD);

        // 표준편차 계산
        int start = prices.size() - PERIOD;
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (int i = start; i < prices.size(); i++) {
            BigDecimal diff = prices.get(i).subtract(mid);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(PERIOD), SCALE, RoundingMode.HALF_UP);
        BigDecimal stddev   = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal band  = stddev.multiply(BigDecimal.valueOf(MULTIPLIER));
        BigDecimal upper = mid.add(band).setScale(2, RoundingMode.HALF_UP);
        BigDecimal lower = mid.subtract(band).setScale(2, RoundingMode.HALF_UP);

        return new BollingerResult(upper, mid.setScale(2, RoundingMode.HALF_UP), lower);
    }

    public record BollingerResult(BigDecimal upper, BigDecimal mid, BigDecimal lower) {}
}
