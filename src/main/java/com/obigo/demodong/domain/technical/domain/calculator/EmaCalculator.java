package com.obigo.demodong.domain.technical.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EMA(지수이동평균) 계산기.
 * k = 2/(n+1) 표준 EMA 공식 사용.
 */
@Component
public class EmaCalculator {

    private static final int SCALE = 10;

    /**
     * EMA 전체 시계열을 반환한다.
     * 리스트 길이 = prices.size() - period + 1 (period번째부터 유효).
     *
     * @param prices 종가 리스트 (오래된 순서 → 최근 순서)
     * @param period EMA 기간
     * @return EMA 시계열. 데이터 부족 시 빈 리스트 반환.
     */
    public List<BigDecimal> calculate(List<BigDecimal> prices, int period) {
        if (prices == null || prices.size() < period) {
            return List.of();
        }

        List<BigDecimal> emas = new ArrayList<>();
        BigDecimal k = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal oneMinusK = BigDecimal.ONE.subtract(k);

        // 초기 EMA = 첫 period개의 SMA
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(prices.get(i));
        }
        BigDecimal initialEma = sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        emas.add(initialEma);

        // EMA(t) = EMA(t-1) × (1-k) + price(t) × k
        for (int i = period; i < prices.size(); i++) {
            BigDecimal prev = emas.get(emas.size() - 1);
            BigDecimal ema = prev.multiply(oneMinusK)
                    .add(prices.get(i).multiply(k))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            emas.add(ema);
        }

        return Collections.unmodifiableList(emas);
    }

    /**
     * 현재 (마지막) EMA 값만 반환한다.
     *
     * @return 데이터 부족 시 null
     */
    public BigDecimal currentEma(List<BigDecimal> prices, int period) {
        List<BigDecimal> emas = calculate(prices, period);
        return emas.isEmpty() ? null : emas.get(emas.size() - 1);
    }
}
