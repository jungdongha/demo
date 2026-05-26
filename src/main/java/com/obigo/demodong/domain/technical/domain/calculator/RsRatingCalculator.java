package com.obigo.demodong.domain.technical.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 종목 수익률과 시장 수익률을 비교하여 0~100 RS Rating을 산출하는 계산기.
 */
@Component
public class RsRatingCalculator {

    private static final BigDecimal MIN_DIFF = new BigDecimal("-20.0");
    private static final BigDecimal MAX_DIFF = new BigDecimal("20.0");
    private static final int NEUTRAL = 50;

    /**
     * RS Rating을 계산한다.
     * 초과수익률 = 종목 수익률 - 시장 수익률
     * 초과수익률 ±20%p 범위를 0~100으로 선형 정규화 (+20% 이상 = 100, -20% 이하 = 0)
     *
     * @param stockReturn 종목 수익률 (%)
     * @param marketReturn 시장 수익률 (%)
     * @return RS Rating (0~100). 둘 중 하나라도 null인 경우 중립(50) 반환.
     */
    public int calculate(BigDecimal stockReturn, BigDecimal marketReturn) {
        if (stockReturn == null || marketReturn == null) {
            return NEUTRAL;
        }

        BigDecimal diff = stockReturn.subtract(marketReturn);

        if (diff.compareTo(MAX_DIFF) >= 0) {
            return 100;
        }
        if (diff.compareTo(MIN_DIFF) <= 0) {
            return 0;
        }

        // 선형 정규화: (diff - MIN_DIFF) / (MAX_DIFF - MIN_DIFF) * 100
        // (diff + 20) / 40 * 100 = (diff + 20) * 2.5
        double diffDouble = diff.doubleValue();
        double rating = (diffDouble + 20.0) * 2.5;

        int finalRating = (int) Math.round(rating);
        return Math.max(0, Math.min(100, finalRating));
    }
}
