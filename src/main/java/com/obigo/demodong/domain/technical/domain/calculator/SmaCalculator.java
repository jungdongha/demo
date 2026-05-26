package com.obigo.demodong.domain.technical.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * SMA(단순이동평균) 계산기.
 */
@Component
public class SmaCalculator {

    private static final int SCALE = 10;

    /**
     * 최근 period개의 SMA를 반환한다.
     *
     * @param prices 종가 리스트 (오래된 순서 → 최근 순서)
     * @param period SMA 기간
     * @return SMA 값. 데이터 부족 시 null.
     */
    public BigDecimal calculate(List<BigDecimal> prices, int period) {
        if (prices == null || prices.size() < period) {
            return null;
        }

        int start = prices.size() - period;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = start; i < prices.size(); i++) {
            sum = sum.add(prices.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
    }
}
