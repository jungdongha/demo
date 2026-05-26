package com.obigo.demodong.domain.technical.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * RSI(상대강도지수) 계산기.
 * Wilder's Smoothing 방식 사용: avgGain/avgLoss를 지수 평활.
 * 기본 기간: 14일.
 */
@Component
public class RsiCalculator {

    private static final int DEFAULT_PERIOD = 14;
    private static final int SCALE = 10;

    public BigDecimal calculate(List<BigDecimal> prices) {
        return calculate(prices, DEFAULT_PERIOD);
    }

    /**
     * RSI를 계산한다.
     *
     * @param prices 종가 리스트 (오래된 순서 → 최근 순서). 최소 period+1개 필요.
     * @param period RSI 기간
     * @return RSI(0~100). 데이터 부족 시 null.
     */
    public BigDecimal calculate(List<BigDecimal> prices, int period) {
        if (prices == null || prices.size() < period + 1) {
            return null;
        }

        // 첫 period개의 변화량으로 초기 평균이익/평균손실 계산
        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        for (int i = 1; i <= period; i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }
        avgGain = avgGain.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);

        // Wilder's Smoothing
        BigDecimal periodMinus1 = BigDecimal.valueOf(period - 1);
        BigDecimal periodBd = BigDecimal.valueOf(period);

        for (int i = period + 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            BigDecimal gain = change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO;

            avgGain = avgGain.multiply(periodMinus1).add(gain)
                    .divide(periodBd, SCALE, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(periodMinus1).add(loss)
                    .divide(periodBd, SCALE, RoundingMode.HALF_UP);
        }

        // 이익도 손실도 없는 경우 (모든 종가 동일) → 중립 50
        if (avgGain.compareTo(BigDecimal.ZERO) == 0 && avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(50);
        }
        // 손실이 없는 경우 → RSI = 100
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal rs = avgGain.divide(avgLoss, SCALE, RoundingMode.HALF_UP);
        // RSI = 100 - 100 / (1 + RS)
        return BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(100)
                        .divide(BigDecimal.ONE.add(rs), SCALE, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
